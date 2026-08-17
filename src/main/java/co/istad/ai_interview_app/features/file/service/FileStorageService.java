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
