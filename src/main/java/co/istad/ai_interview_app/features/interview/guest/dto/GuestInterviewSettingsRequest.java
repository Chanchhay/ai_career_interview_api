package co.istad.ai_interview_app.features.interview.guest.dto;

import co.istad.ai_interview_app.shared.enums.interview.GuestQuestionSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GuestInterviewSettingsRequest(

        boolean enabled,

        /** Zero means guests may read about it but never start one. */
        @Min(value = 0, message = "Attempts cannot be negative")
        @Max(value = 20, message = "20 attempts per guest is the ceiling")
        int maxAttemptsPerGuest,

        @Min(value = 0, message = "Attempts cannot be negative")
        @Max(value = 500, message = "500 attempts per network per day is the ceiling")
        int maxAttemptsPerIpPerDay,

        @NotNull(message = "Choose where guest questions come from")
        GuestQuestionSource questionSource
) {
}
