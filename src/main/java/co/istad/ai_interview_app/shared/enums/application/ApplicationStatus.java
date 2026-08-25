package co.istad.ai_interview_app.shared.enums.application;

/**
 * Where an application stands.
 *
 * <p>{@link #isClosed()} draws the one line the rest of the platform cares
 * about: a closed application is finished with, and no longer occupies the
 * candidate's single live slot for that job.
 */
public enum ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    AI_INTERVIEW_REQUIRED,
    AI_INTERVIEW_IN_PROGRESS,
    AI_INTERVIEW_FAILED,
    MODERATOR_REVIEW_PENDING,
    AI_INTERVIEW_PASSED,
    SHORTLISTED,
    HUMAN_INTERVIEW_SCHEDULED,
    HIRED,
    REJECTED,
    WITHDRAWN;

    /**
     * Whether this application is over.
     *
     * <p>Kept in step with the partial unique index in
     * {@code V19__allow_reapplying_after_a_closed_application.sql}, which
     * enforces the same rule in the database. If one changes the other must:
     * the index is what actually prevents two live applications, and this is
     * what decides whether the API offers to create one.
     */
    public boolean isClosed() {
        return this == REJECTED || this == WITHDRAWN;
    }
}
