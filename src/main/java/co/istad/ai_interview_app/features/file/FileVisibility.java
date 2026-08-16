package co.istad.ai_interview_app.features.file;

/**
 * Decides which read endpoint can serve an object.
 *
 * <p>The bucket itself is always private — visibility only controls whether the
 * redirect-to-presigned-URL endpoint requires an authenticated caller. The value
 * is encoded as the first segment of the object key so a key alone is enough to
 * tell the two apart.
 */
public enum FileVisibility {

    /**
     * Readable by anyone: avatars, company logos, portfolio cover images.
     * Served by {@code GET /api/v1/public/files/**}.
     */
    PUBLIC("public"),

    /**
     * Readable only by an authenticated caller: resumes, company verification
     * documents. Served by {@code GET /api/v1/files/**}.
     */
    PRIVATE("private");

    private final String prefix;

    FileVisibility(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
