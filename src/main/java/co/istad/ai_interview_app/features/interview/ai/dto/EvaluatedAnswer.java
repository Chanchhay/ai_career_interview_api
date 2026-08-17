package co.istad.ai_interview_app.features.interview.ai.dto;

import java.math.BigDecimal;

public record EvaluatedAnswer(
        Long questionId,
        BigDecimal score,
        String feedback,
        /**
         * A strong answer to the question, written for the candidate to read
         * afterwards. Comes from the same call that scores the answer so it can
         * cover what this candidate actually missed.
         */
        String modelAnswer
) {
}
