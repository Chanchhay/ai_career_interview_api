package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import java.util.UUID;

public record SubmittedResumeResponse(
        UUID id,
        String title,
        String resumeFileUrl,
        VisibilityStatus visibility
) {
}
