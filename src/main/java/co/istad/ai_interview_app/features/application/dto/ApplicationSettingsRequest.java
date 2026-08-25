package co.istad.ai_interview_app.features.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ApplicationSettingsRequest(
        /** Zero disables the cooldown; the upper bound is a year. */
        @Min(0) @Max(365) int reapplyCooldownDays
) {
}
