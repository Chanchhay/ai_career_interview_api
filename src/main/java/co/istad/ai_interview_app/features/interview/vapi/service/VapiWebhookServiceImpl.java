package co.istad.ai_interview_app.features.interview.vapi.service;

import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewService;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiTranscriptTurn;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Entry point for Vapi's server events.
 *
 * <p>Vapi is not a user of this platform, so it never carries a Keycloak token.
 * It authenticates with a shared secret instead, checked here rather than in the
 * security filter chain — see the note on the webhook rule in
 * {@code SecurityConfig}.
 *
 * <p>Every handler is written to tolerate repeats. Vapi retries on any non-2xx
 * response, and an interview must not be scored twice because a reply was slow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VapiWebhookServiceImpl implements VapiWebhookService {

    private static final String END_OF_CALL_REPORT = "end-of-call-report";
    private static final String STATUS_UPDATE = "status-update";

    private final AiInterviewService aiInterviewService;

    @Value("${vapi.webhook-secret:}")
    private String webhookSecret;

    @Override
    public void handle(String presentedSecret, JsonNode payload) {
        verifySecret(presentedSecret);

        JsonNode message = payload == null ? null : payload.path("message");
        if (message == null || message.isMissingNode()) {
            log.warn("Vapi webhook received a body with no message envelope");
            return;
        }

        String type = message.path("type").asText("");
        String callId = normalizeBlankToNull(message.path("call").path("id").asText(""));

        if (!hasText(callId)) {
            log.warn("Vapi webhook event type={} carried no call id; ignoring", type);
            return;
        }

        switch (type) {
            case END_OF_CALL_REPORT -> handleEndOfCall(callId, message);
            case STATUS_UPDATE -> handleStatusUpdate(callId, message);
            // transcript / speech-update / tool-calls arrive at high frequency and
            // carry nothing the end-of-call report does not repeat in full.
            default -> log.debug("Ignoring Vapi event type={} for call={}", type, callId);
        }
    }

    private void handleStatusUpdate(String callId, JsonNode message) {
        String status = message.path("status").asText("");
        log.info("Vapi call={} status={}", callId, status);
    }

    private void handleEndOfCall(String callId, JsonNode message) {
        JsonNode artifact = message.path("artifact");
        String transcript = normalizeBlankToNull(artifact.path("transcript").asText(""));
        List<VapiTranscriptTurn> turns = readTurns(artifact.path("messages"));

        log.info(
                "Vapi call={} ended reason={} turns={}",
                callId,
                message.path("endedReason").asText("unknown"),
                turns.size()
        );

        aiInterviewService.completeFromVapiTranscript(callId, transcript, turns);
    }

    /**
     * Reads {@code artifact.messages} into normalised turns.
     *
     * <p>System turns — the prompt Vapi replays back to us — are dropped: they
     * contain the question list verbatim and would otherwise be matched as the
     * assistant asking every question at once.
     */
    private List<VapiTranscriptTurn> readTurns(JsonNode messages) {
        List<VapiTranscriptTurn> turns = new ArrayList<>();
        if (!messages.isArray()) {
            return turns;
        }

        for (JsonNode node : messages) {
            String role = node.path("role").asText("");
            String text = normalizeBlankToNull(node.path("message").asText(""));
            if (!hasText(text)) {
                continue;
            }

            switch (role) {
                case "bot", "assistant" -> turns.add(
                        new VapiTranscriptTurn(VapiTranscriptTurn.ASSISTANT, text)
                );
                case "user" -> turns.add(
                        new VapiTranscriptTurn(VapiTranscriptTurn.USER, text)
                );
                default -> {
                    // system / tool turns carry no interview content
                }
            }
        }

        return turns;
    }

    private void verifySecret(String presentedSecret) {
        if (!hasText(webhookSecret)) {
            // Fail closed: an unconfigured secret must not mean "accept anything".
            log.error("vapi.webhook-secret is not configured; rejecting webhook call");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vapi webhook is not configured");
        }

        byte[] expected = webhookSecret.getBytes(StandardCharsets.UTF_8);
        byte[] presented = presentedSecret == null
                ? new byte[0]
                : presentedSecret.getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expected, presented)) {
            log.warn("Vapi webhook rejected: shared secret did not match");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Vapi webhook secret");
        }
    }
}
