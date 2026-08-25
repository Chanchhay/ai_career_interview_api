package co.istad.ai_interview_app.features.communication.dto;

import jakarta.validation.constraints.Size;

/**
 * Opens a thread with exactly one counterpart, named one of three ways.
 *
 * <p>Naming the application or the company rather than a raw account id is the
 * common case and the safer one: the recipient is derived from a record the
 * moderator is already looking at, so a mistyped id cannot open a thread with
 * an unrelated stranger.
 */
public record CreateConversationRequest(
        /** APPLICATION thread with the candidate who applied. */
        Long applicationId,
        /** GENERAL thread with the recruiter who owns the company. */
        Long companyId,
        /** GENERAL thread with a named account, when neither of the above fits. */
        String recipientKeycloakUserId,
        @Size(max = 200) String title,
        /** Optional opening message, so starting a thread is one request. */
        @Size(max = 4000) String message
) {
}
