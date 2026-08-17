package co.istad.ai_interview_app.features.interview.vapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ties a Vapi call to the interview session it is voicing.
 *
 * <p>Sent by the browser right after {@code vapi.start()} resolves. Without it
 * the webhook has no way back from a call id to a session, because Vapi's
 * events carry the call and nothing of ours.
 */
public record VapiCallBindingRequest(
        @NotBlank
        @Size(max = 100)
        String callId
) {
}
