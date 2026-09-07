package co.istad.ai_interview_app.features.identity.dto;

import java.util.UUID;

public record CurrentUserProfilesResponse(
        UUID jobSeekerProfileId,
        UUID recruiterProfileId,
        UUID moderatorProfileId,
        UUID adminProfileId,
        UUID financeProfileId
) {
}
