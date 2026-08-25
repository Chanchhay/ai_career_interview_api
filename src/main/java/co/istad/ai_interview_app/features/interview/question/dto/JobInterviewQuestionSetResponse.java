package co.istad.ai_interview_app.features.interview.question.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;

import java.util.List;

/**
 * A job's written questions, plus everything the screen needs to explain what
 * will actually be asked.
 *
 * <p>{@code targetQuestionCount} and {@code defaultMaxScore} come from the
 * platform's AI interview settings. They are echoed here so the editor can say
 * "your 3 questions, then 7 generated" without a second request — and so it
 * says the same number the interview will really use.
 */
public record JobInterviewQuestionSetResponse(
        Long jobId,
        String jobTitle,
        ManualQuestionMode mode,
        int targetQuestionCount,
        int defaultMaxScore,
        /** How many the AI would add, given this set and the current mode. */
        int generatedQuestionCount,
        List<InterviewQuestionType> availableTypes,
        List<JobInterviewQuestionResponse> questions
) {
}
