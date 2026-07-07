package co.istad.ai_interview_app.auth.dto;

import lombok.Builder;

@Builder
public record RegisterResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String gender,
        String phoneNumber,
        String registrationSource
) {
}
