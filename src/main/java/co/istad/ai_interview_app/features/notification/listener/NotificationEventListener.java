package co.istad.ai_interview_app.features.notification.listener;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.application.repository.JobApplicationRepository;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.communication.entity.Conversation;
import co.istad.ai_interview_app.features.communication.entity.ConversationParticipant;
import co.istad.ai_interview_app.features.communication.entity.Message;
import co.istad.ai_interview_app.features.communication.repository.ConversationParticipantRepository;
import co.istad.ai_interview_app.features.communication.repository.MessageRepository;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.finance.entity.Invoice;
import co.istad.ai_interview_app.features.finance.repository.InvoiceRepository;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewSessionRepository;
import co.istad.ai_interview_app.features.interview.human.entity.HumanInterview;
import co.istad.ai_interview_app.features.interview.human.repository.HumanInterviewRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserAdminProfileRepository;
import co.istad.ai_interview_app.features.identity.service.UserAccountRoleResolver;
import co.istad.ai_interview_app.features.moderator.repository.ModeratorProfileRepository;
import co.istad.ai_interview_app.features.notification.dto.MessageStreamEvent;
import co.istad.ai_interview_app.features.notification.dto.NewNotification;
import co.istad.ai_interview_app.features.notification.event.NotificationEvents;
import co.istad.ai_interview_app.features.notification.service.NotificationService;
import co.istad.ai_interview_app.features.notification.service.NotificationStreamService;
import co.istad.ai_interview_app.shared.enums.admin.NotificationEventType;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Turns domain events into notifications, and owns every word a notification
 * says.
 *
 * <p>Every handler runs {@code AFTER_COMMIT}: a rolled-back approval must not
 * leave a recruiter told they were approved. That also means the publishing
 * transaction is gone by the time these run, so each opens its own
 * ({@code REQUIRES_NEW}) and re-reads what it needs by id.
 *
 * <p>Handlers never throw. A notification is a side effect of work that has
 * already succeeded and been committed; failing here cannot undo it, and an
 * exception escaping a listener would only surface as noise in the logs of a
 * request that went fine.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final DateTimeFormatter SCHEDULE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy 'at' HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;
    private final CompanyRepository companyRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AiInterviewSessionRepository aiInterviewSessionRepository;
    private final HumanInterviewRepository humanInterviewRepository;
    private final ModeratorProfileRepository moderatorProfileRepository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserAccountRoleResolver roleResolver;
    private final InvoiceRepository invoiceRepository;
    private final CurrentUserAdminProfileRepository adminProfileRepository;

    /* --------------------------------------------- company verification --- */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCompanyVerificationSubmitted(NotificationEvents.CompanyVerificationSubmitted event) {
        safely("company verification submitted", () -> {
            Company company = companyRepository.findById(event.companyId()).orElse(null);
            if (company == null) return;

            notificationService.createAll(moderatorRecipients().stream()
                    .map(moderatorId -> new NewNotification(
                            moderatorId,
                            NotificationEventType.COMPANY_VERIFICATION_SUBMITTED,
                            "Company awaiting verification",
                            "%s submitted their documents for verification.".formatted(company.getName()),
                            "Company",
                            String.valueOf(company.getId()),
                            "/companies/" + company.getId()
                    ))
                    .toList());
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCompanyVerificationDecided(NotificationEvents.CompanyVerificationDecided event) {
        safely("company verification decided", () -> {
            Company company = companyRepository.findById(event.companyId()).orElse(null);
            if (company == null) return;

            Long recruiterId = recruiterUserAccountId(company);
            if (recruiterId == null) return;

            String note = event.note() == null || event.note().isBlank() ? null : event.note();

            NotificationEventType eventType = switch (event.decision()) {
                case APPROVED -> NotificationEventType.COMPANY_VERIFICATION_APPROVED;
                case REJECTED -> NotificationEventType.COMPANY_VERIFICATION_REJECTED;
                case NEEDS_REVISION -> NotificationEventType.COMPANY_VERIFICATION_REVISION_REQUESTED;
            };

            String title = switch (event.decision()) {
                case APPROVED -> "Company verified";
                case REJECTED -> "Company verification rejected";
                case NEEDS_REVISION -> "Changes requested on your company";
            };

            String body = switch (event.decision()) {
                case APPROVED -> "%s is verified. You can now publish jobs.".formatted(company.getName());
                case REJECTED -> "%s was not verified.".formatted(company.getName());
                case NEEDS_REVISION -> "%s needs changes before it can be verified.".formatted(company.getName());
            };

            notificationService.create(new NewNotification(
                    recruiterId,
                    eventType,
                    title,
                    note == null ? body : body + " " + note,
                    "Company",
                    String.valueOf(company.getId()),
                    "/recruiter/company"
            ));
        });
    }

    /* ---------------------------------------------------- applications --- */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onJobApplicationSubmitted(NotificationEvents.JobApplicationSubmitted event) {
        safely("job application submitted", () -> {
            JobApplication application = jobApplicationRepository.findById(event.applicationId()).orElse(null);
            if (application == null) return;

            String jobTitle = application.getJobPost().getTitle();
            List<NewNotification> notifications = new ArrayList<>();

            // The applicant's own receipt. Recruiters are deliberately absent:
            // an application is private until a moderator forwards it, and a
            // notification would leak that it exists.
            Long applicantId = applicantUserAccountId(application);
            if (applicantId != null) {
                notifications.add(new NewNotification(
                        applicantId,
                        NotificationEventType.JOB_APPLICATION_SUBMITTED,
                        "Application submitted",
                        "Your application for %s was received.".formatted(jobTitle),
                        "JobApplication",
                        String.valueOf(application.getId()),
                        "/job-seeker/applications/" + application.getId()
                ));
            }

            moderatorRecipients().forEach(moderatorId -> notifications.add(new NewNotification(
                    moderatorId,
                    NotificationEventType.JOB_APPLICATION_SUBMITTED,
                    "New application to review",
                    "A candidate applied for %s.".formatted(jobTitle),
                    "JobApplication",
                    String.valueOf(application.getId()),
                    "/applications/" + application.getId()
            )));

            notificationService.createAll(notifications);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onJobApplicationStatusChanged(NotificationEvents.JobApplicationStatusChanged event) {
        safely("job application status changed", () -> {
            if (event.previousStatus() == event.newStatus()) return;

            JobApplication application = jobApplicationRepository.findById(event.applicationId()).orElse(null);
            if (application == null) return;

            String jobTitle = application.getJobPost().getTitle();
            List<NewNotification> notifications = new ArrayList<>();

            Long applicantId = applicantUserAccountId(application);
            if (applicantId != null) {
                notifications.add(new NewNotification(
                        applicantId,
                        NotificationEventType.JOB_APPLICATION_STATUS_CHANGED,
                        applicantTitle(event.newStatus()),
                        "Your application for %s is now %s."
                                .formatted(jobTitle, humanize(event.newStatus())),
                        "JobApplication",
                        String.valueOf(application.getId()),
                        "/job-seeker/applications/" + application.getId()
                ));
            }

            notificationService.createAll(notifications);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCandidateForwarded(NotificationEvents.CandidateForwarded event) {
        safely("candidate forwarded", () -> {
            JobApplication application = jobApplicationRepository.findById(event.applicationId()).orElse(null);
            if (application == null) return;

            Long recruiterId = recruiterUserAccountId(application);
            if (recruiterId == null) return;

            notificationService.create(new NewNotification(
                    recruiterId,
                    NotificationEventType.JOB_APPLICATION_STATUS_CHANGED,
                    "Candidate forwarded to you",
                    "A reviewed candidate for %s is ready for you."
                            .formatted(application.getJobPost().getTitle()),
                    "JobApplication",
                    String.valueOf(application.getId()),
                    "/recruiter/forwarded-candidates/" + application.getId()
            ));
        });
    }

    /* ------------------------------------------------------- interviews --- */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAiInterviewCompleted(NotificationEvents.AiInterviewCompleted event) {
        safely("AI interview completed", () -> {
            AiInterviewSession session = aiInterviewSessionRepository.findById(event.sessionId()).orElse(null);
            if (session == null || session.getJobSeeker() == null) return;

            notificationService.create(new NewNotification(
                    session.getJobSeeker().getId(),
                    NotificationEventType.AI_INTERVIEW_COMPLETED,
                    "AI interview complete",
                    "Your interview for %s has been scored.".formatted(session.getJobPost().getTitle()),
                    "AiInterviewSession",
                    String.valueOf(session.getId()),
                    "/job-seeker/interviews/%d/result".formatted(session.getId())
            ));
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onHumanInterviewScheduled(NotificationEvents.HumanInterviewScheduled event) {
        safely("human interview scheduled", () -> {
            HumanInterview interview = humanInterviewRepository.findById(event.humanInterviewId()).orElse(null);
            if (interview == null) return;

            JobApplication application = interview.getApplication();
            Long applicantId = applicantUserAccountId(application);
            if (applicantId == null) return;

            notificationService.create(new NewNotification(
                    applicantId,
                    NotificationEventType.HUMAN_INTERVIEW_SCHEDULED,
                    "Interview scheduled",
                    "Your interview for %s is on %s.".formatted(
                            application.getJobPost().getTitle(),
                            formatSchedule(interview.getScheduledAt())
                    ),
                    "HumanInterview",
                    String.valueOf(interview.getId()),
                    "/job-seeker/applications/" + application.getId()
            ));
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onHumanInterviewRescheduled(NotificationEvents.HumanInterviewRescheduled event) {
        safely("human interview rescheduled", () -> {
            HumanInterview interview = humanInterviewRepository.findById(event.humanInterviewId()).orElse(null);
            if (interview == null) return;

            JobApplication application = interview.getApplication();
            Long applicantId = applicantUserAccountId(application);
            if (applicantId == null) return;

            /*
             * Naming the old time matters more than it looks: the candidate
             * most likely has the original in a calendar, and "moved to X" on
             * its own gives them no way to tell which booking this is about.
             */
            String movedFrom = event.previousScheduledAt() == null
                    ? ""
                    : " It was previously %s.".formatted(formatSchedule(event.previousScheduledAt()));

            notificationService.create(new NewNotification(
                    applicantId,
                    NotificationEventType.HUMAN_INTERVIEW_RESCHEDULED,
                    "Interview moved",
                    "Your interview for %s is now on %s.%s".formatted(
                            application.getJobPost().getTitle(),
                            formatSchedule(interview.getScheduledAt()),
                            movedFrom
                    ),
                    "HumanInterview",
                    String.valueOf(interview.getId()),
                    "/job-seeker/applications/" + application.getId()
            ));
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onHumanInterviewCancelled(NotificationEvents.HumanInterviewCancelled event) {
        safely("human interview cancelled", () -> {
            HumanInterview interview = humanInterviewRepository.findById(event.humanInterviewId()).orElse(null);
            if (interview == null) return;

            JobApplication application = interview.getApplication();
            Long applicantId = applicantUserAccountId(application);
            if (applicantId == null) return;

            notificationService.create(new NewNotification(
                    applicantId,
                    NotificationEventType.HUMAN_INTERVIEW_CANCELLED,
                    "Interview cancelled",
                    "Your interview for %s on %s has been cancelled.".formatted(
                            application.getJobPost().getTitle(),
                            formatSchedule(interview.getScheduledAt())
                    ),
                    "HumanInterview",
                    String.valueOf(interview.getId()),
                    "/job-seeker/applications/" + application.getId()
            ));
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMessageReceived(NotificationEvents.MessageReceived event) {
        safely("message received", () -> {
            Message message = messageRepository.findById(event.messageId()).orElse(null);
            if (message == null) return;

            Conversation conversation = message.getConversation();
            Long senderId = message.getSenderUserAccount().getId();

            List<ConversationParticipant> recipients =
                    participantRepository.findRecipients(conversation.getId(), senderId);

            /*
             * Every recipient is told the transcript changed, muted or not — an
             * open chat has to fill in while you are looking at it, and muting
             * only asks not to be interrupted. Sent before the notifications so
             * a slow notification write cannot delay what is on screen.
             */
            MessageStreamEvent streamEvent = new MessageStreamEvent(
                    conversation.getId(),
                    message.getId(),
                    senderId,
                    message.getSentAt()
            );

            recipients.forEach(participant -> notificationStreamService.pushMessage(
                    participant.getUserAccount().getId(),
                    streamEvent
            ));

            List<NewNotification> notifications = recipients.stream()
                    // A muted thread still delivers the message; it just stops
                    // announcing it. Muting is about interruption, not access.
                    .filter(participant -> !Boolean.TRUE.equals(participant.getMuted()))
                    .map(participant -> new NewNotification(
                            participant.getUserAccount().getId(),
                            NotificationEventType.MESSAGE_RECEIVED,
                            "New message",
                            preview(conversation.getTitle(), message.getContent()),
                            "Conversation",
                            String.valueOf(conversation.getId()),
                            // Resolved per recipient: each front end mounts
                            // messaging under its own prefix, and one shared
                            // path would dead-end in two of the three.
                            roleResolver.messagesPath(
                                    participant.getUserAccount().getId(),
                                    conversation.getId()
                            )
                    ))
                    .toList();

            notificationService.createAll(notifications);
        });
    }

    /* ---------------------------------------------------------- invoices --- */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInvoiceIssued(NotificationEvents.InvoiceIssued event) {
        safely("invoice issued", () -> notifyInvoiceRecipient(
                event.invoiceId(),
                NotificationEventType.INVOICE_ISSUED,
                "Invoice issued",
                invoice -> "Invoice %s for %s %s is due %s.".formatted(
                        invoice.getInvoiceNo(),
                        invoice.getCurrency(),
                        invoice.getTotalAmount().toPlainString(),
                        formatSchedule(invoice.getDueAt())
                )
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInvoicePaid(NotificationEvents.InvoicePaid event) {
        safely("invoice paid", () -> notifyInvoiceRecipient(
                event.invoiceId(),
                NotificationEventType.INVOICE_PAID,
                "Invoice paid",
                invoice -> "Invoice %s is settled in full. Thank you.".formatted(invoice.getInvoiceNo())
        ));
    }

    /**
     * Both invoice notifications address the recruiter who owns the billed
     * company — the only account on the platform that represents the payer.
     */
    private void notifyInvoiceRecipient(
            Long invoiceId,
            NotificationEventType eventType,
            String title,
            java.util.function.Function<Invoice, String> body
    ) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) return;

        Long recruiterId = Optional.ofNullable(invoice.getCompany().getRecruiterProfile())
                .map(profile -> profile.getUserAccount())
                .map(account -> account.getId())
                .orElse(null);

        if (recruiterId == null) return;

        notificationService.create(new NewNotification(
                recruiterId,
                eventType,
                title,
                body.apply(invoice),
                "Invoice",
                String.valueOf(invoice.getId()),
                "/recruiter/invoices/" + invoice.getId()
        ));
    }

    /* ---------------------------------------------------------- helpers --- */

    /**
     * Everyone who works the review queue: moderators, plus administrators, who
     * reach the same screens through the role hierarchy. Deduplicated because
     * one account can hold both profiles.
     */
    private List<Long> moderatorRecipients() {
        return Stream.concat(
                        moderatorProfileRepository.findUserAccountIdsByStatus(ProfileStatus.ACTIVE).stream(),
                        adminProfileRepository.findUserAccountIdsByStatus(ProfileStatus.ACTIVE).stream()
                )
                .distinct()
                .toList();
    }

    private Long recruiterUserAccountId(Company company) {
        return Optional.ofNullable(company.getRecruiterProfile())
                .map(profile -> profile.getUserAccount())
                .map(account -> account.getId())
                .orElse(null);
    }

    private Long recruiterUserAccountId(JobApplication application) {
        return Optional.ofNullable(application.getJobPost())
                .map(jobPost -> jobPost.getRecruiterProfile())
                .map(profile -> profile.getUserAccount())
                .map(account -> account.getId())
                .orElse(null);
    }

    private Long applicantUserAccountId(JobApplication application) {
        return Optional.ofNullable(application.getJobSeekerProfile())
                .map(profile -> profile.getUserAccount())
                .map(account -> account.getId())
                .orElse(null);
    }

    /** A heading that reads as news, rather than repeating the status verbatim. */
    private String applicantTitle(ApplicationStatus status) {
        return switch (status) {
            case SHORTLISTED -> "You have been shortlisted";
            case HUMAN_INTERVIEW_SCHEDULED -> "Interview scheduled";
            case HIRED -> "You got the job";
            case REJECTED -> "Application closed";
            case AI_INTERVIEW_REQUIRED -> "AI interview required";
            case AI_INTERVIEW_PASSED -> "AI interview passed";
            case AI_INTERVIEW_FAILED -> "AI interview not passed";
            default -> "Application updated";
        };
    }

    /** First line of the message, trimmed so a long paragraph stays a preview. */
    private String preview(String conversationTitle, String content) {
        String text = content == null ? "" : content.strip().replaceAll("\\s+", " ");
        String trimmed = text.length() <= 140 ? text : text.substring(0, 139) + "…";
        String prefix = conversationTitle == null || conversationTitle.isBlank()
                ? ""
                : conversationTitle + ": ";

        return prefix + trimmed;
    }

    private String humanize(ApplicationStatus status) {
        return status.name().replace('_', ' ').toLowerCase();
    }

    private String formatSchedule(Instant scheduledAt) {
        return scheduledAt == null ? "a date to be confirmed" : SCHEDULE_FORMAT.format(scheduledAt);
    }

    private void safely(String description, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.error("Failed to publish notifications for {}", description, exception);
        }
    }
}
