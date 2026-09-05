package co.istad.ai_interview_app.features.interview.guest;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewAvailabilityResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewStartResponse;
import co.istad.ai_interview_app.features.interview.guest.service.GuestInterviewService;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiCallBindingRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.VoiceTranscriptRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trying an AI interview without an account.
 *
 * <p>Under {@code /api/v1/public/**}, so no token is required — which is the
 * point, and also why every method carries the guest's own token in a header.
 * That header is the only thing that says an interview is this visitor's, so it
 * is treated as a bearer secret: sessions are looked up by id <em>and</em>
 * token, never by id alone.
 */
@RestController
@RequestMapping("/api/v1/public/guest-interviews")
@RequiredArgsConstructor
public class PublicGuestInterviewController {

    private static final String GUEST_TOKEN_HEADER = "X-Guest-Token";

    /*
     * The token header is optional on every method on purpose. A missing token
     * is a visitor who does not own the interview, which the service answers
     * with "not found" — the same answer a wrong token gets, so neither reveals
     * whether the interview exists. Declaring it required would instead raise a
     * framework error before any of that reasoning ran.
     */

    private final GuestInterviewService guestInterviewService;

    /** Whether this visitor can start one, and how many they have left. */
    @GetMapping("/availability")
    public ApiResponse<GuestInterviewAvailabilityResponse> availability(
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken
    ) {
        return ApiResponse.success(guestInterviewService.availability(guestToken));
    }

    /**
     * Starts an interview for a published job.
     *
     * <p>The response carries the guest token, including the one just issued to
     * a first-time visitor. The browser must keep it: it is the only way back to
     * this interview.
     */
    @PostMapping("/jobs/{jobId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GuestInterviewStartResponse> start(
            @PathVariable Long jobId,
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
            HttpServletRequest request
    ) {
        return ApiResponse.success(guestInterviewService.start(jobId, guestToken, clientIp(request)));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<AiInterviewSessionResponse> get(
            @PathVariable Long sessionId,
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken
    ) {
        return ApiResponse.success(guestInterviewService.get(sessionId, guestToken));
    }

    @PostMapping("/{sessionId}/start")
    public ApiResponse<AiInterviewSessionResponse> begin(
            @PathVariable Long sessionId,
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken
    ) {
        return ApiResponse.success(guestInterviewService.begin(sessionId, guestToken));
    }

    @PutMapping("/{sessionId}/questions/{questionId}/answer")
    public ApiResponse<AiInterviewSessionResponse> answer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
            @Valid @RequestBody AiInterviewAnswerRequest request
    ) {
        return ApiResponse.success(
                guestInterviewService.answer(sessionId, questionId, guestToken, request));
    }

    /**
     * Attaches the voice call speaking this interview, so Vapi's webhook can
     * find the session again once the call ends.
     */
    @PutMapping("/{sessionId}/vapi-call")
    public ApiResponse<AiInterviewSessionResponse> bindVoiceCall(
            @PathVariable Long sessionId,
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
            @Valid @RequestBody VapiCallBindingRequest request
    ) {
        return ApiResponse.success(guestInterviewService.bindVoiceCall(sessionId, guestToken, request));
    }

    /**
     * Submits a finished voice call for scoring.
     *
     * <p>The same work Vapi's webhook does, reached from the browser. Both
     * exist because the webhook needs a publicly reachable server and the
     * browser cannot report a call the visitor abandoned by closing the tab.
     */
    @PostMapping("/{sessionId}/transcript")
    public ApiResponse<AiInterviewSessionResponse> submitVoiceTranscript(
            @PathVariable Long sessionId,
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
            @Valid @RequestBody VoiceTranscriptRequest request
    ) {
        return ApiResponse.success(
                guestInterviewService.submitVoiceTranscript(sessionId, guestToken, request));
    }

    @PostMapping("/{sessionId}/complete")
    public ApiResponse<AiInterviewResultResponse> complete(
            @PathVariable Long sessionId,
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken
    ) {
        return ApiResponse.success(guestInterviewService.complete(sessionId, guestToken));
    }

    @GetMapping("/{sessionId}/result")
    public ApiResponse<AiInterviewResultResponse> result(
            @PathVariable Long sessionId,
            @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken
    ) {
        return ApiResponse.success(guestInterviewService.result(sessionId, guestToken));
    }

    /**
     * The caller's address, preferring the first hop in {@code X-Forwarded-For}.
     *
     * <p>Behind the gateway every request appears to come from the gateway, so
     * without this the network cap would count the whole world as one visitor.
     * The header is client-controlled and therefore not trusted for anything
     * but counting.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
