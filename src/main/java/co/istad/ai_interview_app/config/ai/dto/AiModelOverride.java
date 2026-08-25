package co.istad.ai_interview_app.config.ai.dto;

import co.istad.ai_interview_app.shared.enums.admin.AiTask;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A per-task model choice. A blank {@code model} means "use the default model",
 * which is how the console clears an override.
 */
public record AiModelOverride(

        @NotNull(message = "Task is required")
        AiTask task,

        @Size(max = 120, message = "Model name must be at most 120 characters")
        String model
) {
}
