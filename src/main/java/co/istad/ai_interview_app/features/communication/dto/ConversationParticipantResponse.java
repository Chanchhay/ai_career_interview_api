package co.istad.ai_interview_app.features.communication.dto;

import java.time.Instant;

/**
 * Someone in a thread.
 *
 * <p>No real name. Names live in Keycloak, not in this database, and resolving
 * them would mean one admin-API call per participant per inbox page. The rest
 * of the platform already identifies people this way — the moderator review
 * console shows a candidate's headline and position, never their name — and for
 * a moderator-mediated channel that pseudonymity is a feature rather than a
 * shortfall.
 */
public record ConversationParticipantResponse(
        Long userAccountId,
        /** SEEKER, RECRUITER, MODERATOR, FINANCE, ADMIN, or UNKNOWN. */
        String role,
        /** Headline or position where the profile has one, otherwise the role. */
        String displayLabel,
        String avatarUrl,
        /** True for the account reading this response. */
        Boolean self,
        Instant lastReadAt
) {
}
