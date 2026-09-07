package co.istad.ai_interview_app.features.job.dto;

import java.util.UUID;

public record PublicJobCategoryResponse(
        UUID id,
        String name,
        String description,
        UUID parentId,
        String parentName
) {
}
