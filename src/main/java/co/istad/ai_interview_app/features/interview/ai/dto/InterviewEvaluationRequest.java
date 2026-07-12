package co.istad.ai_interview_app.features.interview.ai.dto;

import java.util.List;

public record InterviewEvaluationRequest(
        String jobTitle,
        String jobDescription,
        String experienceLevel,
        List<String> requiredSkills,
        List<AnswerEvaluationInput> answers
) {
}
