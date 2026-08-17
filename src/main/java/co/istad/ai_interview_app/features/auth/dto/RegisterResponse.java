package co.istad.ai_interview_app.features.auth.dto;

import lombok.Builder;

@Builder
public record RegisterResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String gender,
        RegistrationRole role,
        String phoneNumber,
        String registrationSource
) {
}
