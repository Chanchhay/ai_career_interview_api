package co.istad.ai_interview_app.features.application.dto;

import jakarta.validation.constraints.Size;

public record JobApplicationCreateRequest(
        Long resumeId,
        @Size(max = 5000, message = "Cover letter must be at most 5000 characters")
        String coverLetter
) {
}
