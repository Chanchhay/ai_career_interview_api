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
import java.util.UUID;

public interface ModeratorCandidateApplicationService {

    Page<CandidateApplicationListItemResponse> getReviewQueue(CandidateApplicationReviewStatus status, Pageable pageable);

    CandidateApplicationDetailResponse getReviewDetail(UUID applicationId);

    HumanInterviewResponse scheduleHumanInterview(UUID applicationId, HumanInterviewRequest request);

    HumanInterviewResponse rescheduleHumanInterview(UUID interviewId, HumanInterviewRequest request);

    HumanInterviewResponse completeHumanInterview(UUID interviewId, HumanInterviewCompleteRequest request);

    HumanInterviewResponse cancelHumanInterview(UUID interviewId);

    CandidateApplicationReviewResponse approve(UUID applicationId, DecisionRequest request);

    CandidateApplicationReviewResponse reject(UUID applicationId, DecisionRequest request);

    CandidateApplicationReviewResponse forward(UUID applicationId);
}
