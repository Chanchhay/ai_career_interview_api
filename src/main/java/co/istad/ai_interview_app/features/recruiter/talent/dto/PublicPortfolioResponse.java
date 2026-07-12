package co.istad.ai_interview_app.features.recruiter.talent.dto;

import java.time.Instant;
import java.util.List;

public record PublicPortfolioResponse(
        Long id,
        String title,
        String summary,
        String publicUrl,
        Instant publishedAt,
        List<PublicPortfolioProjectResponse> projects
) {
}
