package co.istad.ai_interview_app.features.interview.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiInterviewAnswerRequest(
        @NotBlank(message = "Answer text is required")
        String answerText
) {
}
