package co.istad.ai_interview_app.features.application.dto;

import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;

import java.time.Instant;

public record JobApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        Long resumeId,
        String resumeTitle,
        String coverLetter,
        ApplicationStatus status,
        Instant appliedAt,
        Instant createdAt
) {
}
