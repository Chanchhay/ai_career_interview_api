package co.istad.ai_interview_app.features.moderator.service;

import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationListItemResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationReviewResponse;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewCompleteRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewResponse;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ModeratorCandidateApplicationService {

    Page<CandidateApplicationListItemResponse> getReviewQueue(CandidateApplicationReviewStatus status, Pageable pageable);

    CandidateApplicationDetailResponse getReviewDetail(Long applicationId);

    HumanInterviewResponse scheduleHumanInterview(Long applicationId, HumanInterviewRequest request);

    HumanInterviewResponse rescheduleHumanInterview(Long interviewId, HumanInterviewRequest request);

    HumanInterviewResponse completeHumanInterview(Long interviewId, HumanInterviewCompleteRequest request);

    HumanInterviewResponse cancelHumanInterview(Long interviewId);

    CandidateApplicationReviewResponse approve(Long applicationId, DecisionRequest request);

    CandidateApplicationReviewResponse reject(Long applicationId, DecisionRequest request);

    CandidateApplicationReviewResponse forward(Long applicationId);
}
