package co.istad.ai_interview_app.features.interview.vapi.service;

import co.istad.ai_interview_app.features.interview.vapi.dto.TranscriptSegmentationRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.TranscriptSegmentationResult;

public interface AiInterviewTranscriptSegmenter {

    /**
     * Splits a voice interview transcript back into one answer per question.
     */
    TranscriptSegmentationResult segment(TranscriptSegmentationRequest request);
}
