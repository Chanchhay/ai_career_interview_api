package co.istad.ai_interview_app.features.recruiter.talent.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.mapper.AiInterviewMapper;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewSessionRepository;
import co.istad.ai_interview_app.features.interview.human.repository.HumanInterviewRepository;
import co.istad.ai_interview_app.features.moderator.entity.CandidateApplicationReview;
import co.istad.ai_interview_app.features.moderator.mapper.CandidateApplicationMapper;
import co.istad.ai_interview_app.features.moderator.repository.CandidateApplicationReviewRepository;
import co.istad.ai_interview_app.features.recruiter.talent.dto.ForwardedApplicationResponse;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecruiterForwardedApplicationServiceImpl implements RecruiterForwardedApplicationService {

    private final CandidateApplicationReviewRepository reviewRepository;
    private final AiInterviewSessionRepository aiInterviewSessionRepository;
    private final HumanInterviewRepository humanInterviewRepository;
    private final CandidateApplicationMapper candidateApplicationMapper;
    private final AiInterviewMapper aiInterviewMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ForwardedApplicationResponse> getForwardedApplications() {
        return reviewRepository
                .findAllByReviewStatusAndApplication_JobPost_RecruiterProfile_UserAccount_KeycloakUserIdOrderByForwardedAtDesc(
                        CandidateApplicationReviewStatus.FORWARDED,
                        AuthUtils.extractUserId()
                )
                .stream()
                .map(this::toForwardedResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ForwardedApplicationResponse getForwardedApplication(Long applicationId) {
        CandidateApplicationReview review = reviewRepository
                .findByApplication_IdAndReviewStatusAndApplication_JobPost_RecruiterProfile_UserAccount_KeycloakUserId(
                        applicationId,
                        CandidateApplicationReviewStatus.FORWARDED,
                        AuthUtils.extractUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Forwarded application was not found for authenticated recruiter"
                ));

        return toForwardedResponse(review);
    }

    private ForwardedApplicationResponse toForwardedResponse(CandidateApplicationReview review) {
        JobApplication application = review.getApplication();
        AiInterviewResultResponse aiResult = aiInterviewSessionRepository
                .findFirstByApplication_IdAndStatusOrderByEndedAtDesc(application.getId(), InterviewStatus.COMPLETED)
                .map(aiInterviewMapper::toResultResponse)
                .orElse(null);

        return new ForwardedApplicationResponse(
                candidateApplicationMapper.toApplicationSummary(application),
                candidateApplicationMapper.toCandidateProfile(application.getJobSeekerProfile()),
                candidateApplicationMapper.toSubmittedResume(application.getResume()),
                aiResult,
                humanInterviewRepository.findAllByApplication_IdOrderByScheduledAtDesc(application.getId())
                        .stream()
                        .map(candidateApplicationMapper::toHumanInterviewResponse)
                        .toList(),
                review.getForwardedAt()
        );
    }
}
