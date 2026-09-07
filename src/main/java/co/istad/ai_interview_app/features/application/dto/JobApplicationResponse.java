package co.istad.ai_interview_app.features.application.dto;

import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record JobApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID resumeId,
        String resumeTitle,
        String coverLetter,
        ApplicationStatus status,
        Instant appliedAt,
        Instant createdAt
) {
}
