package co.istad.ai_interview_app.features.interview.question.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import java.util.UUID;

public record JobInterviewQuestionResponse(
        UUID id,
        Integer displayOrder,
        InterviewQuestionType questionType,
        String questionText,
        String expectedAnswer,
        Integer maxScore
) {
}
