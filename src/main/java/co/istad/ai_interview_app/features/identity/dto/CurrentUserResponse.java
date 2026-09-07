package co.istad.ai_interview_app.features.identity.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userAccountId,
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
        /** App-relative avatar URL from whichever profile this account owns; null if none set. */
        String avatarUrl,
        CurrentUserProfilesResponse profiles
) {
}
