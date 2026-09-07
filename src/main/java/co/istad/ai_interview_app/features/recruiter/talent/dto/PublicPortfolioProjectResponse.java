package co.istad.ai_interview_app.features.recruiter.talent.dto;

import java.util.UUID;

public record PublicPortfolioProjectResponse(
        UUID id,
        String title,
        String description,
        String projectUrl,
        String githubUrl,
        String imageUrl,
        String techStack,
        Integer displayOrder
) {
}
