package co.istad.ai_interview_app.features.recruiter.talent.dto;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.moderator.dto.ApplicationSummaryResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateProfileResponse;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewResponse;
import co.istad.ai_interview_app.features.moderator.dto.SubmittedResumeResponse;

import java.time.Instant;
import java.util.List;

public record ForwardedApplicationResponse(
        ApplicationSummaryResponse application,
        CandidateProfileResponse candidate,
        SubmittedResumeResponse submittedResume,
        AiInterviewResultResponse aiResult,
        List<HumanInterviewResponse> humanInterviews,
        Instant forwardedAt
) {
}
