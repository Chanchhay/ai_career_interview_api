package co.istad.ai_interview_app.features.interview.ai.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AiInterviewAnswerResponse(
        UUID id,
        String answerText,
        BigDecimal score,
        String feedback,
        /** Null until the interview is scored; never exposes the private rubric. */
        String modelAnswer
) {
}
