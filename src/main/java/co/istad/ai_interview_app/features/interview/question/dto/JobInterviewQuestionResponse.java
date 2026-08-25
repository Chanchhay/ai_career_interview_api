package co.istad.ai_interview_app.features.interview.question.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;

public record JobInterviewQuestionResponse(
        Long id,
        Integer displayOrder,
        InterviewQuestionType questionType,
        String questionText,
        String expectedAnswer,
        Integer maxScore
) {
}
