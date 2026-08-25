package co.istad.ai_interview_app.shared.enums.seeker;

/**
 * How a resume came to exist, which decides what the platform may do with it.
 *
 * <p>A template resume can be re-rendered from its structured data at any time;
 * an uploaded one is an opaque file the platform stores and serves but never
 * reinterprets. Keeping the two apart in one column avoids inferring it from
 * whether {@code resumeData} happens to be populated.
 */
public enum ResumeSourceType {
    /** Built in the editor from structured data, rendered by the platform. */
    PLATFORM_TEMPLATE,
    /** A PDF or DOCX the job seeker brought. Never parsed, never regenerated. */
    USER_UPLOAD
}
