package co.istad.ai_interview_app.features.interview.vapi;

import co.istad.ai_interview_app.features.interview.vapi.service.VapiWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

/**
 * Receives Vapi server events for voice interviews.
 *
 * <p>Sits under {@code /api/v1/integrations} rather than {@code /api/v1/job-seeker}
 * because the caller is a machine, not a signed-in seeker, and the two must not
 * share an authorization rule.
 */
@RestController
@RequestMapping("/api/v1/integrations/vapi")
@RequiredArgsConstructor
public class VapiWebhookController {

    private final VapiWebhookService vapiWebhookService;

    /**
     * The shared secret arrives as {@code X-Vapi-Secret} when the assistant's
     * server secret is used, and under the header name chosen by the dashboard
     * when a custom credential is used instead. Both are read so either setup
     * works without a code change.
     *
     * <p>Deliberately not {@code Authorization}: that header already means a
     * Keycloak JWT everywhere else in this application, and the resource-server
     * filter would try to decode the secret as one.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Vapi-Secret", required = false) String vapiSecret,
            @RequestHeader(value = "X-Vapi-Webhook-Secret", required = false) String customSecret,
            @RequestBody JsonNode payload
    ) {
        vapiWebhookService.handle(
                hasText(vapiSecret) ? vapiSecret : customSecret,
                payload
        );

        return ResponseEntity.ok().build();
    }
}
