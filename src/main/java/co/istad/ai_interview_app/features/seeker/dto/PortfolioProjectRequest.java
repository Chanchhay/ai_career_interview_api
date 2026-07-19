package co.istad.ai_interview_app.features.seeker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortfolioProjectRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,
        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,
        @Size(max = 500, message = "Project URL must be at most 500 characters")
        String projectUrl,
        @Size(max = 500, message = "GitHub URL must be at most 500 characters")
        String githubUrl,
        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,
        @Size(max = 2000, message = "Tech stack must be at most 2000 characters")
        String techStack,
        Integer displayOrder
) {
}
