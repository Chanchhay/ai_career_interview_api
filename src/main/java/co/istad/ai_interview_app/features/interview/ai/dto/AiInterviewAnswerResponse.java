package co.istad.ai_interview_app.features.interview.ai.dto;

import java.math.BigDecimal;

public record AiInterviewAnswerResponse(
        Long id,
        String answerText,
        BigDecimal score,
        String feedback,
        /** Null until the interview is scored; never exposes the private rubric. */
        String modelAnswer
) {
}
