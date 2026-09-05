package co.istad.ai_interview_app.features.interview.guest.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewAvailabilityResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewStartResponse;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiCallBindingRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.VoiceTranscriptRequest;

/**
 * AI interviews for people who are not signed in.
 *
 * <p>Every method takes the guest's token because there is no authenticated
 * caller to resolve one from. A null or unknown token is a visitor with no
 * interviews, never an error.
 */
public interface GuestInterviewService {

    GuestInterviewAvailabilityResponse availability(String guestToken);

    GuestInterviewStartResponse start(Long jobId, String guestToken, String clientIp);

    AiInterviewSessionResponse get(Long sessionId, String guestToken);

    AiInterviewSessionResponse begin(Long sessionId, String guestToken);

    AiInterviewSessionResponse answer(Long sessionId, Long questionId, String guestToken, AiInterviewAnswerRequest request);

    AiInterviewResultResponse complete(Long sessionId, String guestToken);

    AiInterviewResultResponse result(Long sessionId, String guestToken);

    /* A guest may sit the interview by voice as well as by typing. */

    AiInterviewSessionResponse bindVoiceCall(Long sessionId, String guestToken, VapiCallBindingRequest request);

    AiInterviewSessionResponse submitVoiceTranscript(Long sessionId, String guestToken, VoiceTranscriptRequest request);
}
