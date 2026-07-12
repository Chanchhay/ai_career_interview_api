package co.istad.ai_interview_app.features.interview.ai.dto;

import java.util.List;

public record AiInterviewResultResponse(
        AiInterviewSessionResponse session,
        AiInterviewFeedbackResponse feedback,
        List<AiInterviewQuestionResponse> questions
) {
}
