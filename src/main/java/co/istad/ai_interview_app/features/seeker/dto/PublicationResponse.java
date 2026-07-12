package co.istad.ai_interview_app.features.seeker.dto;

import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;

import java.time.Instant;

public record PublicationResponse(
        String resourceType,
        Long resourceId,
        VisibilityStatus visibility,
        String publicProfileSlug,
        Instant publishedAt
) {
}
