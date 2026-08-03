package co.istad.ai_interview_app.features.company.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;

import java.time.Instant;

public record IndustryResponse(
        Long id,
        String name,
        String description,
        ProfileStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
