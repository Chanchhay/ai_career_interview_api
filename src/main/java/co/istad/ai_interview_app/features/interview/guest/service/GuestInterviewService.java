package co.istad.ai_interview_app.features.interview.guest.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewAvailabilityResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewStartResponse;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiCallBindingRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.VoiceTranscriptRequest;
import java.util.UUID;

/**
 * AI interviews for people who are not signed in.
 *
 * <p>Every method takes the guest's token because there is no authenticated
 * caller to resolve one from. A null or unknown token is a visitor with no
 * interviews, never an error.
 */
public interface GuestInterviewService {

    GuestInterviewAvailabilityResponse availability(String guestToken);

    GuestInterviewStartResponse start(UUID jobId, String guestToken, String clientIp);

    AiInterviewSessionResponse get(UUID sessionId, String guestToken);

    AiInterviewSessionResponse begin(UUID sessionId, String guestToken);

    AiInterviewSessionResponse answer(UUID sessionId, UUID questionId, String guestToken, AiInterviewAnswerRequest request);

    AiInterviewResultResponse complete(UUID sessionId, String guestToken);

    AiInterviewResultResponse result(UUID sessionId, String guestToken);

    /* A guest may sit the interview by voice as well as by typing. */

    AiInterviewSessionResponse bindVoiceCall(UUID sessionId, String guestToken, VapiCallBindingRequest request);

    AiInterviewSessionResponse submitVoiceTranscript(UUID sessionId, String guestToken, VoiceTranscriptRequest request);
}
