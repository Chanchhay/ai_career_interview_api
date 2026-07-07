package co.istad.ai_interview_app.features.identity.dto;

import java.util.List;

public record CurrentUserResponse(
        Long userAccountId,
        String keycloakUserId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String gender,
        String phoneNumber,
        String registrationSource,
        List<String> roles,
        CurrentUserProfilesResponse profiles
) {
}
