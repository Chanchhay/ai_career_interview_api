package co.istad.ai_interview_app.config.ai.dto;

import co.istad.ai_interview_app.shared.enums.admin.AiTask;
import co.istad.ai_interview_app.shared.enums.admin.AiThinking;

import java.time.Instant;
import java.util.List;

/**
 * The live provider settings as the console may see them. The API key is never
 * part of this — only a mask of it, and the flags the console needs to explain
 * where the key in use is coming from.
 */
public record AiProviderConfigResponse(
        String model,
        /** `••••••af31`, or null when no key is stored and the environment's is in use. */
        String apiKeyMask,
        /** Whether a key has been saved through the console at all. */
        boolean apiKeyStored,
        /** Whether the deployment's own environment variable supplies a key as fallback. */
        boolean environmentKeyAvailable,
        /** False when no encryption key is configured, which makes the key field read-only. */
        boolean apiKeyEditable,
        List<AiModelOverride> modelOverrides,
        List<AiTask> availableTasks,
        Double temperature,
        Integer maxOutputTokens,
        Integer timeoutSeconds,
        Integer maxRetries,
        AiThinking thinking,
        List<AiThinking> availableThinkingLevels,
        Instant updatedAt,
        String updatedBy
) {
}
