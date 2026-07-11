package co.istad.ai_interview_app.features.job.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JobPostSkillRequest(
        @NotNull(message = "Skill ID is required")
        Long skillId,

        @Size(max = 255, message = "Required level must be at most 255 characters")
        String requiredLevel
) {
}
