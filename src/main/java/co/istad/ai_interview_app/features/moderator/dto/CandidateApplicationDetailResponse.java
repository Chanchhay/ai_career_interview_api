package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;

import java.util.List;

public record CandidateApplicationDetailResponse(
        ApplicationSummaryResponse application,
        CandidateProfileResponse candidate,
        SubmittedResumeResponse submittedResume,
        CandidateApplicationReviewResponse review,
        AiInterviewResultResponse aiResult,
        List<HumanInterviewResponse> humanInterviews,
        List<ProjectAssignmentSummaryResponse> projectAssignments
) {
}
