package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationSummaryResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        String coverLetter,
        ApplicationStatus status,
        Instant appliedAt
) {
}
