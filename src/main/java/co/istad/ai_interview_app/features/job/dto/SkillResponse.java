package co.istad.ai_interview_app.features.job.dto;

import java.time.Instant;

public record SkillResponse(
        Long id,
        String name,
        String skillType,
        Instant createdAt,
        Instant updatedAt
) {
}
