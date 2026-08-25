package co.istad.ai_interview_app.config.ai.dto;

/** One model the console may offer, as the provider names it. */
public record AiModelOption(
        /** What goes in the settings, e.g. `gemini-3.5-flash` — no `models/` prefix. */
        String id,
        String displayName,
        String description
) {
}
