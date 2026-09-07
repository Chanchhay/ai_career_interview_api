package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiCallBindingRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiTranscriptTurn;
import co.istad.ai_interview_app.features.interview.vapi.dto.VoiceTranscriptRequest;

import java.util.List;
import java.util.UUID;

public interface AiInterviewService {

    AiInterviewSessionResponse createInterviewForJob(UUID jobId);

    AiInterviewSessionResponse createInterviewForApplication(UUID applicationId);

    List<AiInterviewSessionResponse> getMyInterviews();

    AiInterviewSessionResponse getMyInterview(UUID sessionId);

    AiInterviewSessionResponse startInterview(UUID sessionId);

    AiInterviewSessionResponse submitAnswer(UUID sessionId, UUID questionId, AiInterviewAnswerRequest request);

    AiInterviewResultResponse completeInterview(UUID sessionId);

    AiInterviewResultResponse getResult(UUID sessionId);

    /** Binds the Vapi call that is voicing this interview, so its webhook can find the session again. */
    AiInterviewSessionResponse bindVapiCall(UUID sessionId, VapiCallBindingRequest request);

    /**
     * Records a finished voice call and, if the transcript covered every
     * question, scores the interview.
     *
     * <p>Called by Vapi's webhook, so it resolves the session by call id rather
     * than by authenticated seeker, and returns quietly on anything it cannot
     * act on — a webhook has no user to show an error to, and a non-2xx reply
     * only earns a retry of the same unusable event.
     */
    void completeFromVapiTranscript(String vapiCallId, String transcript, List<VapiTranscriptTurn> turns);

    /**
     * Accepts the transcript the browser collected and scores the interview.
     *
     * <p>The same work as {@link #completeFromVapiTranscript}, reached by the
     * candidate's own request rather than by Vapi. Whichever path arrives first
     * scores the interview; the other finds it already done.
     */
    AiInterviewSessionResponse submitVoiceTranscript(UUID sessionId, VoiceTranscriptRequest request);

    /* ------------------------------------------------------------ guests --- */

    /*
     * A guest holds no account, so a token their browser keeps stands in for
     * one. These mirror the seeker methods above and share every internal step
     * with them — the same questions, the same state machine, the same scoring.
     * Only who is allowed to touch the session differs, which is the whole of
     * the difference between a guest and a candidate.
     */

    AiInterviewSessionResponse createGuestInterview(
            UUID jobId,
            String guestToken,
            String guestIpHash,
            ManualQuestionMode modeOverride
    );

    AiInterviewSessionResponse getGuestInterview(UUID sessionId, String guestToken);

    AiInterviewSessionResponse startGuestInterview(UUID sessionId, String guestToken);

    AiInterviewSessionResponse submitGuestAnswer(
            UUID sessionId,
            UUID questionId,
            String guestToken,
            AiInterviewAnswerRequest request
    );

    AiInterviewResultResponse completeGuestInterview(UUID sessionId, String guestToken);

    AiInterviewResultResponse getGuestResult(UUID sessionId, String guestToken);

    /** Attaches the voice call that is speaking a guest's interview. */
    AiInterviewSessionResponse bindGuestVapiCall(
            UUID sessionId,
            String guestToken,
            VapiCallBindingRequest request
    );

    /** Scores a guest's finished voice call from its transcript. */
    AiInterviewSessionResponse submitGuestVoiceTranscript(
            UUID sessionId,
            String guestToken,
            VoiceTranscriptRequest request
    );
}
