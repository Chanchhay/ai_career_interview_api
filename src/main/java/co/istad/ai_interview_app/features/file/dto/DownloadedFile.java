package co.istad.ai_interview_app.features.file.dto;

/**
 * How a stored file should be delivered.
 *
 * <p>Exactly one of {@code content} or {@code redirectUrl} is set:
 *
 * <ul>
 *   <li><strong>content</strong> — the file lives in this platform's object
 *       storage, so the backend reads it and streams the bytes. Authorization is
 *       then re-checked on every fetch rather than baked into a link.
 *   <li><strong>redirectUrl</strong> — the row points at somewhere else entirely.
 *       Resumes predate object storage and may hold a client-supplied URL, and
 *       the platform has no bytes to serve for those. The browser is redirected
 *       instead of the server fetching the address, which would turn this
 *       endpoint into a request forwarder aimed by whoever set the field.
 * </ul>
 */
public record DownloadedFile(
        byte[] content,
        String filename,
        String contentType,
        String redirectUrl
) {

    public static DownloadedFile streamed(byte[] content, String filename, String contentType) {
        return new DownloadedFile(content, filename, contentType, null);
    }

    /** For a row that points outside this platform's storage. */
    public static DownloadedFile redirect(String url) {
        return new DownloadedFile(null, null, null, url);
    }

    public boolean isRedirect() {
        return redirectUrl != null && !redirectUrl.isBlank();
    }
}
