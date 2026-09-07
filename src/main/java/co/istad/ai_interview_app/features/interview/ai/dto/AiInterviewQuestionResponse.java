package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import java.util.UUID;

public record AiInterviewQuestionResponse(
        UUID id,
        Integer displayOrder,
        InterviewQuestionType questionType,
        String questionText,
        Integer maxScore,
        Boolean answered,
        AiInterviewAnswerResponse answer
) {
}
