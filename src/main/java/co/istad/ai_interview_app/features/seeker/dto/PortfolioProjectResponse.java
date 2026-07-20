package co.istad.ai_interview_app.features.seeker.dto;

import java.time.Instant;

public record PortfolioProjectResponse(
        Long id,
        String title,
        String description,
        String projectUrl,
        String githubUrl,
        String imageUrl,
        String techStack,
        Integer displayOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
