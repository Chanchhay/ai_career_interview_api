package co.istad.ai_interview_app.features.file.dto;

/**
 * @param key         object key inside the MinIO bucket
 * @param url         stable app-relative URL that resolves the object; this is
 *                    what callers persist in fields such as {@code logoUrl} or
 *                    {@code resumeFileUrl}
 * @param name        original client-supplied file name
 * @param size        size in bytes
 * @param contentType detected MIME type
 */
public record FileUploadResponse(
        String key,
        String url,
        String name,
        Long size,
        String contentType
) {
}
