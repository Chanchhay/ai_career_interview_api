package co.istad.ai_interview_app.features.seeker.dto;

import java.time.Instant;
import java.util.UUID;

public record PortfolioProjectResponse(
        UUID id,
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
