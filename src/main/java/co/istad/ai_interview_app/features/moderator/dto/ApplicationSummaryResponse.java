package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;

import java.time.Instant;

public record ApplicationSummaryResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String coverLetter,
        ApplicationStatus status,
        Instant appliedAt
) {
}
