package co.istad.ai_interview_app.features.job.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobPostRequest(
        Long categoryId,

        @NotBlank(message = "Job title is required")
        @Size(max = 200, message = "Job title must be at most 200 characters")
        String title,

        @NotBlank(message = "Job description is required")
        String description,

        @Size(max = 255, message = "Location must be at most 255 characters")
        String location,

        @Size(max = 255, message = "Job type must be at most 255 characters")
        String jobType,

        @Size(max = 255, message = "Work mode must be at most 255 characters")
        String workMode,

        @PositiveOrZero(message = "Minimum salary must be zero or greater")
        BigDecimal salaryMin,

        @PositiveOrZero(message = "Maximum salary must be zero or greater")
        BigDecimal salaryMax,

        @Size(max = 255, message = "Experience level must be at most 255 characters")
        String experienceLevel,

        @Future(message = "Expiration date must be in the future")
        Instant expiredAt,

        @Valid
        List<JobPostSectionRequest> sections,

        @Valid
        List<JobPostSkillRequest> skills
) {
}
