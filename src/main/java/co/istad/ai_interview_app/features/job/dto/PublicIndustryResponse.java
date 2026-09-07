package co.istad.ai_interview_app.features.job.dto;

import java.util.UUID;

public record PublicIndustryResponse(
        UUID id,
        String name,
        String description,
        UUID parentId,
        String parentName
) {
}
