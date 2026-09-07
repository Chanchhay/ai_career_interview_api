package co.istad.ai_interview_app.features.job.dto;

import java.time.Instant;
import java.util.UUID;

public record JobCategoryResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        UUID parentId,
        String parentName
) {
}
