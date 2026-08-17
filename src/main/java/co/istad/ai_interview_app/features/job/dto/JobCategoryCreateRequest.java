package co.istad.ai_interview_app.features.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobCategoryCreateRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 150, message = "Category name must be at most 150 characters")
        String name,

        String description
) {
}
