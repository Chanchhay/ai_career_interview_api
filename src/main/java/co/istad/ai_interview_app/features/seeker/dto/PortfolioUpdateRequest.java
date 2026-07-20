package co.istad.ai_interview_app.features.seeker.dto;

import jakarta.validation.constraints.Size;

public record PortfolioUpdateRequest(
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,
        @Size(max = 5000, message = "Summary must be at most 5000 characters")
        String summary,
        @Size(max = 500, message = "Public URL must be at most 500 characters")
        String publicUrl
) {
}
