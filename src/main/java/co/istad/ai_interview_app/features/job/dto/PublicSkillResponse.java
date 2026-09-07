package co.istad.ai_interview_app.features.job.dto;

import java.util.UUID;

public record PublicSkillResponse(
        UUID id,
        String name,
        String skillType,
        UUID parentId,
        String parentName
) {
}
