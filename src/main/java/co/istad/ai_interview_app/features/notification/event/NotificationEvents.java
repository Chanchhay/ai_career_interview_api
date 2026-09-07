package co.istad.ai_interview_app.features.notification.event;

import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.moderation.ModerationDecision;

import java.time.Instant;
import java.util.UUID;

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
    public record CompanyVerificationSubmitted(UUID companyId) {
    }

    /** A moderator approved, rejected, or asked for changes. The recruiter is told. */
    public record CompanyVerificationDecided(UUID companyId, ModerationDecision decision, String note) {
    }

    /** A job seeker applied. The moderator queue is told; the applicant gets a receipt. */
    public record JobApplicationSubmitted(UUID applicationId) {
    }

    /** An application moved. Tells the applicant only — see {@link CandidateForwarded}. */
    public record JobApplicationStatusChanged(
            UUID applicationId,
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
    public record CandidateForwarded(UUID applicationId) {
    }

    /** An invoice was issued to a company. Its recruiter is told what they owe. */
    public record InvoiceIssued(UUID invoiceId) {
    }

    /** A recruiter reported a hire. Moderators are told to review it. */
    public record HireReported(UUID hiringRecordId) {
    }

    /** An invoice was settled in full. The recruiter gets the receipt. */
    public record InvoicePaid(UUID invoiceId) {
    }

    /** Someone sent a message. Every other participant still in the thread is told. */
    public record ConversationChanged(UUID conversationId) {}

    public record MessageReceived(UUID messageId) {
    }

    /** A candidate finished an AI interview. */
    public record AiInterviewCompleted(UUID sessionId) {
    }

    /** A moderator booked a human interview. The candidate is told. */
    public record HumanInterviewScheduled(UUID humanInterviewId) {
    }

    /**
     * A moderator moved a booked interview.
     *
     * <p>Carries the old time as well as the id: a candidate who has the
     * original in their calendar needs to know what it moved from, not just
     * what it moved to.
     */
    public record HumanInterviewRescheduled(UUID humanInterviewId, Instant previousScheduledAt) {
    }

    /** A moderator called off a booked interview. The candidate is told. */
    public record HumanInterviewCancelled(UUID humanInterviewId) {
    }
}
