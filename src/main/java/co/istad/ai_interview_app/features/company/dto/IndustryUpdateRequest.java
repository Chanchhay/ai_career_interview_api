package co.istad.ai_interview_app.features.company.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IndustryUpdateRequest(
        @NotBlank(message = "Industry name is required")
        @Size(max = 150, message = "Industry name must be at most 150 characters")
        String name,

        String description,

        ProfileStatus status
) {
}
