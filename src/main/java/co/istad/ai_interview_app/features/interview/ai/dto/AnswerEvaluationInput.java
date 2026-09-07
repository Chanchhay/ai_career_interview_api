package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import java.util.UUID;

public record AnswerEvaluationInput(
        UUID questionId,
        Integer order,
        InterviewQuestionType questionType,
        String question,
        String rubric,
        Integer maxScore,
        String answer
) {
}
