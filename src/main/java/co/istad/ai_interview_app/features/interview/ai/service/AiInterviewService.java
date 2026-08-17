package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiCallBindingRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiTranscriptTurn;
import co.istad.ai_interview_app.features.interview.vapi.dto.VoiceTranscriptRequest;

import java.util.List;

public interface AiInterviewService {

    AiInterviewSessionResponse createInterviewForJob(Long jobId);

    AiInterviewSessionResponse createInterviewForApplication(Long applicationId);

    List<AiInterviewSessionResponse> getMyInterviews();

    AiInterviewSessionResponse getMyInterview(Long sessionId);

    AiInterviewSessionResponse startInterview(Long sessionId);

    AiInterviewSessionResponse submitAnswer(Long sessionId, Long questionId, AiInterviewAnswerRequest request);

    AiInterviewResultResponse completeInterview(Long sessionId);

    AiInterviewResultResponse getResult(Long sessionId);

    /** Binds the Vapi call that is voicing this interview, so its webhook can find the session again. */
    AiInterviewSessionResponse bindVapiCall(Long sessionId, VapiCallBindingRequest request);

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
    AiInterviewSessionResponse submitVoiceTranscript(Long sessionId, VoiceTranscriptRequest request);
}
