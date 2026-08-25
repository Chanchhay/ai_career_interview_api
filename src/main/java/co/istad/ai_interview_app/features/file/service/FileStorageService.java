package co.istad.ai_interview_app.features.file.service;

import co.istad.ai_interview_app.features.file.FileVisibility;
import co.istad.ai_interview_app.features.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Validates and stores an object in MinIO.
     *
     * @return the stored key plus the app-relative URL callers should persist
     */
    FileUploadResponse upload(MultipartFile file, FileVisibility visibility);

    /**
     * Stores bytes the application produced itself.
     *
     * <p>Separate from {@link #upload} because a generated PDF has no
     * {@code MultipartFile} behind it, and because the caller has already
     * decided what the content is — there is no client-supplied type to
     * validate against an allow-list here.
     *
     * @param extension file extension without the dot, used for the object key
     */
    FileUploadResponse store(
            byte[] content,
            String extension,
            String contentType,
            FileVisibility visibility
    );

    /**
     * Reads an object back into memory.
     *
     * <p>For the cases that must not hand out a presigned URL at all — where the
     * backend streams the bytes itself after checking ownership.
     */
    byte[] read(String key);

    /**
     * Builds a short-lived presigned GET URL for an object.
     *
     * @param key object key, without a leading slash
     */
    String presignedUrl(String key);

    /**
     * Removes an object. Succeeds silently when the object is already gone.
     */
    void delete(String key);
}
