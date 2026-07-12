package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HumanInterviewCompleteRequest(
        @NotNull(message = "Interview result is required")
        InterviewResult result,
        @Size(max = 5000, message = "Note must be at most 5000 characters")
        String note
) {
}
