package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.project.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectAssignmentSummaryResponse(
        UUID id,
        String title,
        String description,
        Instant deadlineAt,
        ProjectStatus status
) {
}
