package co.istad.ai_interview_app.features.file.service;

import co.istad.ai_interview_app.features.file.FileVisibility;
import co.istad.ai_interview_app.features.file.dto.FileUploadResponse;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    /**
     * Mirrors the accept lists used by the frontend dropzone. Anything outside
     * this map is rejected rather than stored under a guessed extension.
     */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", "pdf",
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/svg+xml", "svg"
    );

    private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ofPattern("yyyy/MM");

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.max-file-size:5242880}")
    private long maxFileSize;

    @Value("${minio.presigned-expiry-minutes:15}")
    private int presignedExpiryMinutes;

    /**
     * Creates the bucket on first boot so a fresh MinIO instance needs no manual
     * setup. Failure is logged rather than fatal — the app still serves every
     * endpoint that does not touch storage.
     */
    @PostConstruct
    void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket '{}'", bucket);
            }
        } catch (Exception e) {
            log.error("Could not verify MinIO bucket '{}': {}", bucket, e.getMessage());
        }
    }

    @Override
    public FileUploadResponse upload(MultipartFile file, FileVisibility visibility) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }
        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(
                    HttpStatus.CONTENT_TOO_LARGE,
                    "Files must be %d MB or smaller".formatted(maxFileSize / (1024 * 1024))
            );
        }

        String contentType = file.getContentType();
        String extension = contentType == null ? null : ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF, PNG, JPG, WebP, and SVG files are accepted"
            );
        }

        String key = "%s/%s/%s.%s".formatted(
                visibility.prefix(),
                LocalDate.now().format(KEY_DATE),
                UUID.randomUUID(),
                extension
        );

        try (InputStream stream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            // null part size lets the SDK pick one for us
                            .stream(stream, file.getSize(), null)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO upload failed for key '{}'", key, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to store the file");
        }

        return new FileUploadResponse(
                key,
                readUrl(key, visibility),
                file.getOriginalFilename(),
                file.getSize(),
                contentType
        );
    }

    @Override
    public FileUploadResponse store(
            byte[] content,
            String extension,
            String contentType,
            FileVisibility visibility
    ) {
        if (content == null || content.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to store");
        }

        if (content.length > maxFileSize) {
            throw new ResponseStatusException(
                    HttpStatus.CONTENT_TOO_LARGE,
                    "Files must be %d MB or smaller".formatted(maxFileSize / (1024 * 1024))
            );
        }

        String key = "%s/%s/%s.%s".formatted(
                visibility.prefix(),
                LocalDate.now().format(KEY_DATE),
                UUID.randomUUID(),
                extension
        );

        try (InputStream stream = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(stream, (long) content.length, null)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO store failed for key '{}'", key, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to store the file");
        }

        return new FileUploadResponse(
                key,
                readUrl(key, visibility),
                null,
                (long) content.length,
                contentType
        );
    }

    @Override
    public byte[] read(String key) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(key).build()
        )) {
            return stream.readAllBytes();
        } catch (Exception e) {
            log.error("Could not read key '{}'", key, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to read the file");
        }
    }

    @Override
    public String presignedUrl(String key) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(presignedExpiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Could not presign key '{}'", key, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to read the file");
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(key).build()
            );
        } catch (Exception e) {
            log.error("Could not delete key '{}'", key, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to delete the file");
        }
    }

    /**
     * The URL persisted by callers. It is app-relative and stable, so rotating
     * MinIO hosts or credentials never invalidates stored rows — only the
     * short-lived redirect target changes.
     */
    private String readUrl(String key, FileVisibility visibility) {
        return visibility == FileVisibility.PUBLIC
                ? "/api/v1/public/files/" + key
                : "/api/v1/files/" + key;
    }
}
