package co.istad.ai_interview_app.features.seeker.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;

import java.time.Instant;
import java.util.List;

public record PortfolioResponse(
        Long id,
        String title,
        String summary,
        String publicUrl,
        VisibilityStatus visibility,
        Instant publishedAt,
        ProfileStatus status,
        List<PortfolioProjectResponse> projects,
        Instant createdAt,
        Instant updatedAt
) {
}
