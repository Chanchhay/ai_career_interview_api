package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;

public record SubmittedResumeResponse(
        Long id,
        String title,
        String resumeFileUrl,
        VisibilityStatus visibility
) {
}
