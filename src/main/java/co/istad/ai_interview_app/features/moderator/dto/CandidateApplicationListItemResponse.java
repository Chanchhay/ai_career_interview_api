package co.istad.ai_interview_app.features.moderator.dto;

public record CandidateApplicationListItemResponse(
        ApplicationSummaryResponse application,
        CandidateProfileResponse candidate,
        SubmittedResumeResponse submittedResume,
        CandidateApplicationReviewResponse review
) {
}
