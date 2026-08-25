package co.istad.ai_interview_app.config.ai.dto;

import co.istad.ai_interview_app.shared.enums.admin.AiTask;
import co.istad.ai_interview_app.shared.enums.admin.AiThinking;

import java.time.Duration;
import java.util.Map;

/**
 * The settings as the chat client factory consumes them: every value already
 * resolved, so no caller has to know whether it came from the database or the
 * deployment's configuration.
 */
public record AiRuntimeSettings(
        String apiKey,
        boolean apiKeyFromConsole,
        String model,
        Map<AiTask, String> modelByTask,
        Double temperature,
        Integer maxOutputTokens,
        Duration timeout,
        Integer maxRetries,
        AiThinking thinking
) {

    public String modelFor(AiTask task) {
        return modelByTask.getOrDefault(task, model);
    }
}
