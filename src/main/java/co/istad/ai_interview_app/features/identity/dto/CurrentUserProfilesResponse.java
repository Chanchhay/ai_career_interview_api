package co.istad.ai_interview_app.features.identity.dto;

public record CurrentUserProfilesResponse(
        Long jobSeekerProfileId,
        Long recruiterProfileId,
        Long moderatorProfileId,
        Long adminProfileId,
        Long financeProfileId
) {
}
