package co.istad.ai_interview_app.shared.enums.finance;

/**
 * A hire moves through review before it bills anyone.
 *
 * <p>The recruiter knows who they hired, but they are also the party the
 * commission is charged to. Confirmation by a moderator is what turns a claim
 * into a billable event.
 */
public enum HiringRecordStatus {
    /** Reported by the recruiter, not yet reviewed. Bills nobody. */
    REPORTED,
    /** Confirmed by a moderator. This is what creates the commission. */
    CONFIRMED,
    /** Reviewed and dismissed — a mistaken or withdrawn report. */
    REJECTED
}
