package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;

import java.time.Instant;
import java.util.List;

/**
 * The current generation settings, plus everything the console needs to render
 * an editor for them — the full type vocabulary included, so the UI never has
 * to hard-code a copy of {@link InterviewQuestionType}.
 */
public record AiInterviewConfigResponse(
        Integer questionCount,
        Integer maxScorePerQuestion,
        List<QuestionTypeAllocation> typeDistribution,
        String additionalInstructions,
        List<InterviewQuestionType> availableTypes,
        Instant updatedAt,
        String updatedBy
) {
}
