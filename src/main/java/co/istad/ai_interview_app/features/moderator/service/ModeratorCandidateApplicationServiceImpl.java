package co.istad.ai_interview_app.features.moderator.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import co.istad.ai_interview_app.features.interview.ai.mapper.AiInterviewMapper;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewSessionRepository;
import co.istad.ai_interview_app.features.interview.human.entity.HumanInterview;
import co.istad.ai_interview_app.features.interview.human.repository.HumanInterviewRepository;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationListItemResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationReviewResponse;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewCompleteRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewResponse;
import co.istad.ai_interview_app.features.moderator.entity.CandidateApplicationReview;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.moderator.mapper.CandidateApplicationMapper;
import co.istad.ai_interview_app.features.moderator.repository.CandidateApplicationReviewRepository;
import co.istad.ai_interview_app.features.project.repository.ProjectAssignmentRepository;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import org.springframework.context.ApplicationEventPublisher;
import co.istad.ai_interview_app.features.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModeratorCandidateApplicationServiceImpl implements ModeratorCandidateApplicationService {

    private final CandidateApplicationReviewRepository reviewRepository;
    private final HumanInterviewRepository humanInterviewRepository;
    private final AiInterviewSessionRepository aiInterviewSessionRepository;
    private final ProjectAssignmentRepository projectAssignmentRepository;
    private final AuthenticatedModeratorProfileResolver moderatorProfileResolver;
    private final CandidateApplicationMapper mapper;
    private final AiInterviewMapper aiInterviewMapper;
    private final ApplicationEventPublisher events;

    /** Human-interview states that mean "booked, not yet held". */
    private static final Set<InterviewStatus> OUTSTANDING_INTERVIEWS =
            Set.of(InterviewStatus.PENDING, InterviewStatus.IN_PROGRESS);

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateApplicationListItemResponse> getReviewQueue(
            CandidateApplicationReviewStatus status,
            Pageable pageable
    ) {
        Page<CandidateApplicationReview> reviews = status == null
                ? reviewRepository.findAll(pageable)
                : reviewRepository.findAllByReviewStatus(status, pageable);

        return reviews.map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateApplicationDetailResponse getReviewDetail(Long applicationId) {
        CandidateApplicationReview review = resolveReview(applicationId);
        JobApplication application = review.getApplication();

        AiInterviewResultResponse aiResult = aiInterviewSessionRepository
                .findFirstByApplication_IdAndStatusOrderByEndedAtDesc(applicationId, InterviewStatus.COMPLETED)
                .map(aiInterviewMapper::toResultResponse)
                .orElse(null);

        return new CandidateApplicationDetailResponse(
                mapper.toApplicationSummary(application),
                mapper.toCandidateProfile(application.getJobSeekerProfile()),
                mapper.toSubmittedResume(application.getResume()),
                mapper.toReviewResponse(review),
                aiResult,
                humanInterviewRepository.findAllByApplication_IdOrderByScheduledAtDesc(applicationId)
                        .stream()
                        .map(mapper::toHumanInterviewResponse)
                        .toList(),
                projectAssignmentRepository.findAllByApplication_IdOrderByCreatedAtDesc(applicationId)
                        .stream()
                        .map(mapper::toProjectAssignmentResponse)
                        .toList()
        );
    }

    @Override
    @Transactional
    public HumanInterviewResponse scheduleHumanInterview(Long applicationId, HumanInterviewRequest request) {
        ModeratorProfile moderator = moderatorProfileResolver.resolve();
        CandidateApplicationReview review = resolveReview(applicationId);
        JobApplication application = review.getApplication();
        validateApplicationOpenForModeratorAction(application);

        HumanInterview interview = new HumanInterview();
        interview.setApplication(application);
        interview.setModerator(moderator);
        interview.setScheduledAt(request.scheduledAt());
        interview.setMeetingUrl(normalizeBlankToNull(request.meetingUrl()));
        interview.setStatus(InterviewStatus.PENDING);
        humanInterviewRepository.save(interview);

        review.setModerator(moderator);
        review.setReviewStatus(CandidateApplicationReviewStatus.HUMAN_INTERVIEW_SCHEDULED);
        application.setStatus(ApplicationStatus.HUMAN_INTERVIEW_SCHEDULED);

        // Only the interview event: it names the date and the meeting, which is
        // strictly more than a bare status change would tell the candidate.
        events.publishEvent(new NotificationEvents.HumanInterviewScheduled(interview.getId()));

        return mapper.toHumanInterviewResponse(interview);
    }

    @Override
    @Transactional
    public HumanInterviewResponse rescheduleHumanInterview(Long interviewId, HumanInterviewRequest request) {
        HumanInterview interview = resolveMyHumanInterview(interviewId);
        if (interview.getStatus() == InterviewStatus.COMPLETED || interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed or cancelled interviews cannot be rescheduled");
        }

        Instant previousScheduledAt = interview.getScheduledAt();
        interview.setScheduledAt(request.scheduledAt());
        interview.setMeetingUrl(normalizeBlankToNull(request.meetingUrl()));

        events.publishEvent(new NotificationEvents.HumanInterviewRescheduled(
                interview.getId(), previousScheduledAt));

        return mapper.toHumanInterviewResponse(interview);
    }

    @Override
    @Transactional
    public HumanInterviewResponse completeHumanInterview(Long interviewId, HumanInterviewCompleteRequest request) {
        HumanInterview interview = resolveMyHumanInterview(interviewId);
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled interviews cannot be completed");
        }
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            return mapper.toHumanInterviewResponse(interview);
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setResult(request.result());
        interview.setNote(normalizeBlankToNull(request.note()));
        interview.setCompletedAt(Instant.now());

        CandidateApplicationReview review = resolveReview(interview.getApplication().getId());
        review.setReviewStatus(CandidateApplicationReviewStatus.DECISION_PENDING);

        return mapper.toHumanInterviewResponse(interview);
    }

    @Override
    @Transactional
    public HumanInterviewResponse cancelHumanInterview(Long interviewId) {
        HumanInterview interview = resolveMyHumanInterview(interviewId);
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed interviews cannot be cancelled");
        }
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            return mapper.toHumanInterviewResponse(interview);
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancelledAt(Instant.now());

        // Below the early return above, so cancelling an already-cancelled
        // interview stays silent rather than telling the candidate twice.
        events.publishEvent(new NotificationEvents.HumanInterviewCancelled(interview.getId()));

        return mapper.toHumanInterviewResponse(interview);
    }

    @Override
    @Transactional
    public CandidateApplicationReviewResponse approve(Long applicationId, DecisionRequest request) {
        ModeratorProfile moderator = moderatorProfileResolver.resolve();
        CandidateApplicationReview review = resolveReview(applicationId);
        JobApplication application = review.getApplication();

        if (application.getStatus() == ApplicationStatus.WITHDRAWN || application.getStatus() == ApplicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejected or withdrawn applications cannot be approved");
        }
        if (review.getReviewStatus() == CandidateApplicationReviewStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejected applications cannot be approved");
        }
        AiInterviewSession aiInterview = requireCompletedAiInterview(application);

        /*
         * A failed AI interview blocks approval outright.
         *
         * <p>NEEDS_REVIEW deliberately does not. That result exists to say the
         * machine could not decide and a person should — refusing it would
         * strand exactly the candidates moderator review is for. Only an
         * explicit FAILED is treated as a decision.
         *
         * <p>A completed session with no result at all is an anomaly rather
         * than a verdict, so it is not read as a failure either.
         */
        if (aiInterview.getResult() == InterviewResult.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This candidate did not pass the AI interview and cannot be approved"
            );
        }
        /*
         * A human interview gates approval only once one has been booked.
         *
         * <p>Requiring one unconditionally made approval impossible for the
         * ordinary case — a candidate who passed the AI interview and was never
         * asked to a human one had no path forward at all, because nothing
         * schedules an interview automatically.
         *
         * <p>What is still refused is deciding while an interview the moderator
         * booked has not happened: approving in that window contradicts the act
         * of booking it. Completed and cancelled are both settled.
         */
        if (humanInterviewRepository.existsByApplication_IdAndStatusIn(applicationId, OUTSTANDING_INTERVIEWS)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Complete or cancel the scheduled human interview before approving"
            );
        }

        review.setModerator(moderator);
        review.setReviewStatus(CandidateApplicationReviewStatus.APPROVED);
        review.setDecisionNote(normalizeBlankToNull(request.decisionNote()));
        review.setReviewedAt(Instant.now());
        review.setApprovedAt(Instant.now());
        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(ApplicationStatus.SHORTLISTED);

        events.publishEvent(new NotificationEvents.JobApplicationStatusChanged(
                application.getId(), previousStatus, ApplicationStatus.SHORTLISTED));

        return mapper.toReviewResponse(review);
    }

    @Override
    @Transactional
    public CandidateApplicationReviewResponse reject(Long applicationId, DecisionRequest request) {
        ModeratorProfile moderator = moderatorProfileResolver.resolve();
        CandidateApplicationReview review = resolveReview(applicationId);
        JobApplication application = review.getApplication();
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Withdrawn applications cannot be rejected");
        }

        review.setModerator(moderator);
        review.setReviewStatus(CandidateApplicationReviewStatus.REJECTED);
        review.setDecisionNote(normalizeBlankToNull(request.decisionNote()));
        review.setReviewedAt(Instant.now());
        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(ApplicationStatus.REJECTED);
        application.setClosedAt(Instant.now());

        events.publishEvent(new NotificationEvents.JobApplicationStatusChanged(
                application.getId(), previousStatus, ApplicationStatus.REJECTED));

        return mapper.toReviewResponse(review);
    }

    @Override
    @Transactional
    public CandidateApplicationReviewResponse forward(Long applicationId) {
        CandidateApplicationReview review = resolveReview(applicationId);
        if (review.getReviewStatus() == CandidateApplicationReviewStatus.FORWARDED) {
            return mapper.toReviewResponse(review);
        }
        if (review.getReviewStatus() != CandidateApplicationReviewStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Forwarding requires moderator approval");
        }
        if (review.getApplication().getStatus() == ApplicationStatus.WITHDRAWN
                || review.getApplication().getStatus() == ApplicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejected or withdrawn applications cannot be forwarded");
        }

        review.setReviewStatus(CandidateApplicationReviewStatus.FORWARDED);
        review.setForwardedAt(Instant.now());

        events.publishEvent(new NotificationEvents.CandidateForwarded(review.getApplication().getId()));

        return mapper.toReviewResponse(review);
    }

    private CandidateApplicationListItemResponse toListItem(CandidateApplicationReview review) {
        JobApplication application = review.getApplication();
        return new CandidateApplicationListItemResponse(
                mapper.toApplicationSummary(application),
                mapper.toCandidateProfile(application.getJobSeekerProfile()),
                mapper.toSubmittedResume(application.getResume()),
                mapper.toReviewResponse(review)
        );
    }

    private CandidateApplicationReview resolveReview(Long applicationId) {
        return reviewRepository.findWithApplicationByApplication_Id(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Candidate application review was not found"
                ));
    }

    private HumanInterview resolveMyHumanInterview(Long interviewId) {
        return humanInterviewRepository.findByIdAndModerator_UserAccount_KeycloakUserId(interviewId, AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Human interview was not found for authenticated moderator"
                ));
    }

    /**
     * The candidate's most recent completed AI interview.
     *
     * <p>Most recent rather than any: a candidate who sat the interview twice
     * should be judged on the attempt that stands, not on whichever row the
     * database returned first.
     */
    private Optional<AiInterviewSession> latestCompletedAiInterview(Long applicationId) {
        return aiInterviewSessionRepository
                .findFirstByApplication_IdAndStatusOrderByEndedAtDesc(applicationId, InterviewStatus.COMPLETED);
    }

    /**
     * The completed AI interview approval is gated on, or a refusal that says
     * which of several very different situations the reviewer is actually in.
     *
     * <p>One message for all of them — "Approval requires a completed AI
     * interview" — is what made this expensive to diagnose: a candidate who
     * never started, one who is halfway through, and one whose finished
     * interview simply was not attached to their application all read the same,
     * and only the last is a defect.
     *
     * <p>That last case is repaired rather than reported. A finished interview
     * for this job by this candidate already counts towards this application —
     * that is the rule {@code adoptPracticeInterviews} applies when they apply.
     * Refusing here only because the interview happened at an awkward moment
     * would contradict it.
     */
    private AiInterviewSession requireCompletedAiInterview(JobApplication application) {
        Optional<AiInterviewSession> attached = latestCompletedAiInterview(application.getId());
        if (attached.isPresent()) return attached.get();

        Optional<AiInterviewSession> orphan = aiInterviewSessionRepository
                .findAllByJobPost_IdAndJobSeeker_IdAndApplicationIsNull(
                        application.getJobPost().getId(),
                        application.getJobSeekerProfile().getUserAccount().getId()
                )
                .stream()
                .filter(session -> session.getStatus() == InterviewStatus.COMPLETED)
                .max(Comparator.comparing(
                        AiInterviewSession::getEndedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ));

        if (orphan.isPresent()) {
            AiInterviewSession session = orphan.get();
            log.info(
                    "Attaching AI interview session {} to application {} during approval — "
                            + "it was completed for this job but never linked",
                    session.getId(),
                    application.getId()
            );
            session.setApplication(application);

            return session;
        }

        List<AiInterviewSession> unfinished = aiInterviewSessionRepository
                .findAllByApplication_Id(application.getId());

        if (!unfinished.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This candidate's AI interview has not finished yet, so there is "
                            + "no result to approve on."
            );
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This candidate has not taken the AI interview for this job yet."
        );
    }

    private void validateApplicationOpenForModeratorAction(JobApplication application) {
        List<ApplicationStatus> closedStatuses = List.of(
                ApplicationStatus.WITHDRAWN,
                ApplicationStatus.REJECTED,
                ApplicationStatus.HIRED
        );
        if (closedStatuses.contains(application.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This application is closed");
        }
    }
}
