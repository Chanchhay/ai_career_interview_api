package co.istad.ai_interview_app.features.admin.users.dto;

import co.istad.ai_interview_app.shared.enums.account.ManageableRole;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The complete role set the account should end up with, not a delta. The
 * service works out which roles to add and which to remove, so two
 * administrators editing the same user cannot compose their changes into a set
 * neither of them intended.
 */
public record AdminUserRolesRequest(
        @NotNull List<ManageableRole> roles
) {
}
