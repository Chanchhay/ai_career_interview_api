package co.istad.ai_interview_app.features.file;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.file.dto.FileUploadResponse;
import co.istad.ai_interview_app.features.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

/**
 * Upload and authenticated read access for objects stored in MinIO.
 *
 * <p>Reads answer with a 302 to a short-lived presigned URL rather than piping
 * bytes through the API, so the bucket stays private while MinIO still serves
 * the transfer.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "visibility", defaultValue = "PRIVATE") FileVisibility visibility
    ) {
        return ApiResponse.success(fileStorageService.upload(file, visibility));
    }

    @GetMapping("/{*key}")
    public ResponseEntity<Void> read(@PathVariable String key) {
        String objectKey = normalizeKey(key);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .location(URI.create(fileStorageService.presignedUrl(objectKey)))
                .build();
    }

    @DeleteMapping("/{*key}")
    public ApiResponse<Void> delete(@PathVariable String key) {
        fileStorageService.delete(normalizeKey(key));
        return ApiResponse.success(null);
    }

    /**
     * {@code {*key}} captures the trailing path with its leading slash. Traversal
     * segments are rejected outright so a key can never escape its prefix.
     */
    static String normalizeKey(String key) {
        String normalized = key == null ? "" : key.startsWith("/") ? key.substring(1) : key;
        if (normalized.isBlank() || normalized.contains("..")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return normalized;
    }
}
