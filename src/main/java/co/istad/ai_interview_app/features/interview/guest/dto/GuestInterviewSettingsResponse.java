package co.istad.ai_interview_app.features.interview.guest.dto;

import co.istad.ai_interview_app.shared.enums.interview.GuestQuestionSource;

/**
 * The rules for interviews taken by people who are not signed in.
 *
 * <p>Every AI interview costs money to generate and to score, and a guest has
 * no account to hold responsible — so the limits here are the only thing
 * standing between a demo and an open tab on the platform's AI spend.
 */
public record GuestInterviewSettingsResponse(
        boolean enabled,
        /** How many interviews one guest may take. */
        int maxAttemptsPerGuest,
        /**
         * How many interviews may start from one network in a day. The backstop
         * for the fact that a guest can clear their browser and look new.
         */
        int maxAttemptsPerIpPerDay,
        GuestQuestionSource questionSource
) {
}
