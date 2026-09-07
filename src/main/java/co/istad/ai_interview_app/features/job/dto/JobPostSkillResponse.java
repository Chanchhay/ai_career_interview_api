package co.istad.ai_interview_app.features.job.dto;

import java.util.UUID;

public record JobPostSkillResponse(
        UUID id,
        UUID skillId,
        String skillName,
        String skillType,
        String requiredLevel
) {
}
