package co.istad.ai_interview_app.features.identity.dto;

import java.util.List;

public record CurrentUserResponse(
        Long userAccountId,
        String keycloakUserId,
        String email,
        String fullName,
        List<String> roles,
        CurrentUserProfilesResponse profiles
) {
}
