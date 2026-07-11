package co.istad.ai_interview_app.features.job.dto;

import co.istad.ai_interview_app.shared.enums.job.JobPostSectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record JobPostSectionRequest(
        @NotNull(message = "Section type is required")
        JobPostSectionType sectionType,

        @NotBlank(message = "Section title is required")
        @Size(max = 150, message = "Section title must be at most 150 characters")
        String title,

        @NotBlank(message = "Section content is required")
        String contentMarkdown,

        String contentText,

        @NotNull(message = "Section display order is required")
        @PositiveOrZero(message = "Section display order must be zero or greater")
        Integer displayOrder
) {
}
