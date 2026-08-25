package co.istad.ai_interview_app.features.notification.event;

import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.moderation.ModerationDecision;

import java.time.Instant;

/**
 * The domain events that produce a notification.
 *
 * <p>Each carries identifiers only, never entities. Listeners run after the
 * publishing transaction has committed and outside its persistence context, so
 * a detached entity handed across that boundary would fail the moment a lazy
 * association was touched. Re-reading by id also guarantees the listener sees
 * committed state rather than whatever was in memory when the event was built.
 */
public final class NotificationEvents {

    private NotificationEvents() {
    }

    /** A recruiter sent their company for verification. Moderators are told. */
    public record CompanyVerificationSubmitted(Long companyId) {
    }

    /** A moderator approved, rejected, or asked for changes. The recruiter is told. */
    public record CompanyVerificationDecided(Long companyId, ModerationDecision decision, String note) {
    }

    /** A job seeker applied. The moderator queue is told; the applicant gets a receipt. */
    public record JobApplicationSubmitted(Long applicationId) {
    }

    /** An application moved. Tells the applicant only — see {@link CandidateForwarded}. */
    public record JobApplicationStatusChanged(
            Long applicationId,
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus
    ) {
    }

    /**
     * A moderator forwarded a reviewed candidate to the recruiter.
     *
     * <p>Separate from {@link JobApplicationStatusChanged} because forwarding —
     * not approval — is what makes the candidate visible to the recruiter.
     * Approval already sets the application to SHORTLISTED, so notifying the
     * recruiter on that status would tell them about a candidate the forwarded
     * -applications endpoint still refuses to show them.
     */
    public record CandidateForwarded(Long applicationId) {
    }

    /** An invoice was issued to a company. Its recruiter is told what they owe. */
    public record InvoiceIssued(Long invoiceId) {
    }

    /** An invoice was settled in full. The recruiter gets the receipt. */
    public record InvoicePaid(Long invoiceId) {
    }

    /** Someone sent a message. Every other participant still in the thread is told. */
    public record MessageReceived(Long messageId) {
    }

    /** A candidate finished an AI interview. */
    public record AiInterviewCompleted(Long sessionId) {
    }

    /** A moderator booked a human interview. The candidate is told. */
    public record HumanInterviewScheduled(Long humanInterviewId) {
    }

    /**
     * A moderator moved a booked interview.
     *
     * <p>Carries the old time as well as the id: a candidate who has the
     * original in their calendar needs to know what it moved from, not just
     * what it moved to.
     */
    public record HumanInterviewRescheduled(Long humanInterviewId, Instant previousScheduledAt) {
    }

    /** A moderator called off a booked interview. The candidate is told. */
    public record HumanInterviewCancelled(Long humanInterviewId) {
    }
}
