package co.istad.ai_interview_app.features.interview.ai.dto;

import java.util.List;

public record AiInterviewResultResponse(
        AiInterviewSessionResponse session,
        AiInterviewFeedbackResponse feedback
//        List<AiInterviewQuestionResponse> questions
) {

    public List<AiInterviewQuestionResponse> questions() {
        return session == null ? List.of() : session.questions();
    }
}
