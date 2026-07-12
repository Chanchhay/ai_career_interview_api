package co.istad.ai_interview_app.features.seeker.dto;

import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import jakarta.validation.constraints.NotNull;

public record PublicationRequest(
        @NotNull
        VisibilityStatus visibility
) {
}
