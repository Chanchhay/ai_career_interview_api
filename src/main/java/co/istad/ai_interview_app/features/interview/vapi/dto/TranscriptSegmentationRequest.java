package co.istad.ai_interview_app.features.interview.vapi.dto;

import java.util.List;

/**
 * A whole voice interview handed to Gemini to be split back into answers.
 *
 * @param questions the questions that were meant to be asked, in order
 * @param transcript the full conversation, one labelled turn per line
 */
public record TranscriptSegmentationRequest(
        List<TranscriptQuestion> questions,
        String transcript
) {

    public record TranscriptQuestion(
            Long questionId,
            Integer displayOrder,
            String questionText
    ) {
    }
}
