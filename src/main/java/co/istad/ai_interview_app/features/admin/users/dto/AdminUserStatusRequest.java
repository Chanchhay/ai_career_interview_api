package co.istad.ai_interview_app.features.admin.users.dto;

import jakarta.validation.constraints.Size;

/**
 * Why the account is being suspended or reinstated. Optional, but it is the
 * only free text the audit trail will carry for the action, so the UI asks for
 * it.
 */
public record AdminUserStatusRequest(
        @Size(max = 500) String reason
) {
}
