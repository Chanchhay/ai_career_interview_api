package co.istad.ai_interview_app.features.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Opens a support thread with the moderator team.
 *
 * <p>The only way a seeker or recruiter can start a conversation. Every other
 * thread is opened by a moderator, and this one is addressed to the moderator
 * pool rather than to a person the caller chooses — so it gives users a voice
 * without giving them a way to reach each other.
 */
public record OpenSupportRequest(
        @Size(max = 200) String subject,
        @NotBlank @Size(max = 4000) String message
) {
}
