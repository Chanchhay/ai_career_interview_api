package co.istad.ai_interview_app.features.recruiter.talent.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicPortfolioResponse(
        UUID id,
        String title,
        String summary,
        String publicUrl,
        Map<String, Object> portfolioData,
        Instant publishedAt,
        List<PublicPortfolioProjectResponse> projects
) {
}
