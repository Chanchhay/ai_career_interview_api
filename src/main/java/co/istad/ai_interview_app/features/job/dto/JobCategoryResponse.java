package co.istad.ai_interview_app.features.job.dto;

import java.time.Instant;

public record JobCategoryResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
