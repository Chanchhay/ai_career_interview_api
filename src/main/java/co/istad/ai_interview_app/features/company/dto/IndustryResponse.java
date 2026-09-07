package co.istad.ai_interview_app.features.company.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record IndustryResponse(
        UUID id,
        String name,
        String description,
        ProfileStatus status,
        Instant createdAt,
        Instant updatedAt,
        UUID parentId,
        String parentName
) {
}
