package co.istad.ai_interview_app.features.interview.vapi.service;

import co.istad.ai_interview_app.features.interview.vapi.dto.TranscriptSegmentationRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.TranscriptSegmentationResult;
import co.istad.ai_interview_app.shared.exception.GeminiGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rebuilds per-question answers from a voice interview transcript.
 *
 * <p>Replaces matching answers to questions by conversational turn taking. That
 * approach treated every assistant turn as a question boundary, so a two word
 * acknowledgement split one answer across two questions, and a candidate who
 * paused mid sentence had the halves filed under different questions.
 *
 * <p>Reading a transcript and attributing speech to the question it addresses is
 * a language problem, not a bookkeeping one, so it is given to the model that is
 * already scoring the interview.
 */
@Component
@RequiredArgsConstructor
public class AiInterviewTranscriptSegmenterImpl implements AiInterviewTranscriptSegmenter {

    private final ChatClient geminiChatClient;

    @Override
    public TranscriptSegmentationResult segment(TranscriptSegmentationRequest request) {
        String questionList = request.questions()
                .stream()
                .map(question -> "questionId=%d (order %d): %s".formatted(
                        question.questionId(),
                        question.displayOrder(),
                        question.questionText()
                ))
                .collect(Collectors.joining("\n"));

        TranscriptSegmentationResult result = geminiChatClient
                .prompt()
                .system("""
                        You reconstruct a spoken job interview from its transcript.

                        You are given the questions that were meant to be asked and
                        the raw transcript of the call. Return the candidate's
                        answer to each question.

                        Requirements:
                        - Return exactly one entry per questionId given, and never
                          invent a questionId.
                        - Attribute speech by what it is about, not by where it
                          falls in the transcript. The interviewer may have asked
                          questions out of order, repeated one, or interrupted.
                        - Stitch fragments together. Speech recognition splits a
                          single answer across several turns, sometimes separated
                          by the interviewer acknowledging it.
                        - Use only the candidate's own words. Never include the
                          interviewer's speech, and never write the answer yourself.
                        - Speech recognition errors are expected. Keep the words as
                          transcribed rather than correcting them into something
                          the candidate may not have said.
                        - If the candidate never addressed a question, set answered
                          to false and leave answerText empty. Do not guess, and do
                          not reuse another question's answer.
                        - Set answered to false when the only speech for a question
                          is filler such as "yes", "okay" or "let's go".
                        """)
                .user(user -> user
                        .text("""
                                Questions:
                                {questions}

                                Transcript:
                                {transcript}
                                """)
                        .param("questions", questionList)
                        .param("transcript", request.transcript())
                )
                .call()
                .entity(
                        TranscriptSegmentationResult.class,
                        specification -> specification
                                .useProviderStructuredOutput()
                                .validateSchema()
                );

        validate(request, result);

        return result;
    }

    private void validate(
            TranscriptSegmentationRequest request,
            TranscriptSegmentationResult result
    ) {
        if (result == null || result.answers() == null) {
            throw new GeminiGenerationException("Gemini returned no transcript segmentation");
        }

        Set<Long> expectedQuestionIds = request.questions()
                .stream()
                .map(TranscriptSegmentationRequest.TranscriptQuestion::questionId)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> segmentedQuestionIds = new HashSet<>();
        List<TranscriptSegmentationResult.SegmentedAnswer> answers = result.answers();

        for (TranscriptSegmentationResult.SegmentedAnswer answer : answers) {
            if (answer == null
                    || answer.questionId() == null
                    || !expectedQuestionIds.contains(answer.questionId())
                    || !segmentedQuestionIds.add(answer.questionId())) {
                throw new GeminiGenerationException("Gemini segmented an unexpected question");
            }
        }

        if (!segmentedQuestionIds.equals(expectedQuestionIds)) {
            throw new GeminiGenerationException("Gemini did not segment every question");
        }
    }
}
