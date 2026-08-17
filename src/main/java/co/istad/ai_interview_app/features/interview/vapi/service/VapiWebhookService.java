package co.istad.ai_interview_app.features.interview.vapi.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface VapiWebhookService {

    /**
     * @param presentedSecret the shared secret from the request headers
     * @param payload         the raw Vapi server-event body
     */
    void handle(String presentedSecret, JsonNode payload);
}
