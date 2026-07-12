package co.istad.ai_interview_app.features.recruiter.talent.dto;

public record PublicPortfolioProjectResponse(
        Long id,
        String title,
        String description,
        String projectUrl,
        String githubUrl,
        String imageUrl,
        String techStack,
        Integer displayOrder
) {
}
