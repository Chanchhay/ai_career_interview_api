package co.istad.ai_interview_app.features.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillCreateRequest(
        @NotBlank(message = "Skill name is required")
        @Size(max = 100, message = "Skill name must be at most 100 characters")
        String name,

        @Size(max = 50, message = "Skill type must be at most 50 characters")
        String skillType
) {
}
