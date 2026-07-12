package co.istad.ai_interview_app.features.interview.ai.dto;

import java.math.BigDecimal;

public record EvaluatedAnswer(
        Long questionId,
        BigDecimal score,
        String feedback
) {
}
