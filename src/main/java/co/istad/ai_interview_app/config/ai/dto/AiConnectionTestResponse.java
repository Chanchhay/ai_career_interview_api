package co.istad.ai_interview_app.config.ai.dto;

public record AiConnectionTestResponse(
        boolean success,
        String model,
        /** Where the key came from: "console" or "environment". */
        String keySource,
        Long latencyMillis,
        /** The provider's own words when it fails, so a bad key reads as a bad key. */
        String message
) {
}
