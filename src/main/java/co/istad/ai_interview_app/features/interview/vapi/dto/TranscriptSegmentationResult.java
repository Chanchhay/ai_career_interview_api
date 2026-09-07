package co.istad.ai_interview_app.features.interview.vapi.dto;

import java.util.List;
import java.util.UUID;

/**
 * Gemini's reconstruction of what the candidate answered to each question.
 */
public record TranscriptSegmentationResult(
        List<SegmentedAnswer> answers
) {

    /**
     * @param questionId  the question this answer belongs to
     * @param answerText  the candidate's answer, stitched together from however
     *                    many fragments the transcript split it into; null or
     *                    blank when the question was never answered
     * @param answered    false when the candidate never answered this question,
     *                    so a missing answer is stated rather than inferred from
     *                    an empty string
     */
    public record SegmentedAnswer(
            UUID questionId,
            String answerText,
            Boolean answered
    ) {
    }
}
