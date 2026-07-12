package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;

public record GeneratedQuestion(
        Integer order,
        InterviewQuestionType type,
        String question,
        String rubric,
        Integer maxScore
) {
}