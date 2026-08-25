package co.istad.ai_interview_app.features.application.dto;

public record ApplicationSettingsResponse(
        /**
         * Days a candidate must wait before applying again to a job they were
         * rejected from. Zero switches the cooldown off entirely.
         */
        int reapplyCooldownDays
) {
}
