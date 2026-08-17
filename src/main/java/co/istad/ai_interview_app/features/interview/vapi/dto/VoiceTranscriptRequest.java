package co.istad.ai_interview_app.features.interview.vapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The transcript the browser collected during a voice interview.
 *
 * <p>Posted when the call ends, alongside Vapi's own end-of-call webhook. Two
 * paths for the same content: the webhook needs a publicly reachable server,
 * which local development does not have, and the browser cannot report a call
 * the candidate abandoned by closing the tab. Whichever arrives first scores the
 * interview; the other finds it already done.
 */
public record VoiceTranscriptRequest(
        @NotEmpty
        @Size(max = 500)
        List<@Valid Turn> turns
) {

    public record Turn(
            @NotBlank
            @Size(max = 40)
            String role,

            @NotBlank
            @Size(max = 10_000)
            String text
    ) {
    }
}
