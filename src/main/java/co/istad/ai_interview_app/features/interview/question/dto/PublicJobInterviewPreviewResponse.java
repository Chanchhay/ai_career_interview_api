package co.istad.ai_interview_app.features.interview.question.dto;

import java.util.List;
import java.util.UUID;

/**
 * What the interview for one published job will cover, read before a visitor
 * commits to sitting it.
 *
 * <p>{@code questionCount} is how many questions the interview really asks;
 * {@code questions} holds only the ones an administrator wrote. They differ
 * whenever the AI is topping the set up, so a screen that wants to say "and N
 * more" must subtract rather than trust the list length.
 *
 * <p>The generated questions are absent because they do not exist yet — they
 * are written when the session starts, and are different for every candidate.
 */
public record PublicJobInterviewPreviewResponse(
        UUID jobId,
        String jobTitle,
        /** Written plus generated: the real length of the interview. */
        int questionCount,
        /** A rough sitting time, or null when there is nothing to estimate. */
        Integer estimatedMinutes,
        List<PublicJobInterviewQuestionResponse> questions
) {
}
