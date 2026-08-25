package co.istad.ai_interview_app.config.ai.dto;

import jakarta.validation.constraints.Size;

/**
 * Tries a model and key before they are saved. Both are optional: what is left
 * out falls back to what is stored, so the button works both for checking the
 * live settings and for vetting a key that has just been typed in.
 */
public record AiConnectionTestRequest(

        @Size(max = 120, message = "Model name must be at most 120 characters")
        String model,

        @Size(max = 200, message = "API key must be at most 200 characters")
        String apiKey
) {
}
