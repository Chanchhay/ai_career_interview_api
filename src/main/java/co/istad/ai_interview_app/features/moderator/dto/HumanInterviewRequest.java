package co.istad.ai_interview_app.features.moderator.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record HumanInterviewRequest(
        @NotNull(message = "Scheduled time is required")
        @Future(message = "Scheduled time must be in the future")
        Instant scheduledAt,
        @NotBlank(message = "Meeting URL is required")
        @Size(max = 500, message = "Meeting URL must be at most 500 characters")
        String meetingUrl
) {
}
