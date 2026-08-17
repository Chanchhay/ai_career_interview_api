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
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

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

        return mapper.toHumanInterviewResponse(interview);
    }

    @Override
    @Transactional
    public HumanInterviewResponse rescheduleHumanInterview(Long interviewId, HumanInterviewRequest request) {
        HumanInterview interview = resolveMyHumanInterview(interviewId);
        if (interview.getStatus() == InterviewStatus.COMPLETED || interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed or cancelled interviews cannot be rescheduled");
        }

        interview.setScheduledAt(request.scheduledAt());
        interview.setMeetingUrl(normalizeBlankToNull(request.meetingUrl()));
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
        if (!hasCompletedAiInterview(applicationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval requires a completed AI interview");
        }
        if (!humanInterviewRepository.existsByApplication_IdAndStatus(applicationId, InterviewStatus.COMPLETED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval requires a completed human interview");
        }

        review.setModerator(moderator);
        review.setReviewStatus(CandidateApplicationReviewStatus.APPROVED);
        review.setDecisionNote(normalizeBlankToNull(request.decisionNote()));
        review.setReviewedAt(Instant.now());
        review.setApprovedAt(Instant.now());
        application.setStatus(ApplicationStatus.SHORTLISTED);

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
        application.setStatus(ApplicationStatus.REJECTED);

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

    private boolean hasCompletedAiInterview(Long applicationId) {
        return aiInterviewSessionRepository
                .findFirstByApplication_IdAndStatusOrderByEndedAtDesc(applicationId, InterviewStatus.COMPLETED)
                .isPresent();
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
