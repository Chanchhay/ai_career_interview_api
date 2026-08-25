package co.istad.ai_interview_app.config.ai.dto;

import co.istad.ai_interview_app.shared.enums.admin.AiThinking;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * What the console sends when it changes how the platform talks to the model
 * provider.
 *
 * <p>{@code apiKey} is write-only and three-state on purpose: omitted (null)
 * keeps the stored key, a value replaces it, and {@code clearApiKey} drops it so
 * the deployment's own environment variable takes over again. A read never
 * returns it, so "leave it alone" has to be expressible without echoing the
 * secret back.
 */
public record AiProviderConfigRequest(

        @NotBlank(message = "Default model is required")
        @Size(max = 120, message = "Model name must be at most 120 characters")
        String model,

        @Size(max = 200, message = "API key must be at most 200 characters")
        String apiKey,

        boolean clearApiKey,

        @Valid
        List<AiModelOverride> modelOverrides,

        @NotNull(message = "Temperature is required")
        @DecimalMin(value = "0.0", message = "Temperature cannot be negative")
        @DecimalMax(value = "2.0", message = "Temperature may not exceed 2.0")
        Double temperature,

        @NotNull(message = "Max output tokens is required")
        @Min(value = 256, message = "Max output tokens must be at least 256")
        @Max(value = 65536, message = "Max output tokens may not exceed 65536")
        Integer maxOutputTokens,

        @NotNull(message = "Request timeout is required")
        @Min(value = 5, message = "Request timeout must be at least 5 seconds")
        @Max(value = 600, message = "Request timeout may not exceed 600 seconds")
        Integer timeoutSeconds,

        @NotNull(message = "Max retries is required")
        @Min(value = 0, message = "Max retries cannot be negative")
        @Max(value = 5, message = "Max retries may not exceed 5")
        Integer maxRetries,

        @NotNull(message = "Thinking setting is required")
        AiThinking thinking
) {
}
