package co.istad.ai_interview_app.features.admin.users.dto;

import co.istad.ai_interview_app.shared.enums.account.AccountStatus;
import co.istad.ai_interview_app.shared.enums.account.ManageableRole;

import java.time.Instant;
import java.util.List;

/**
 * A platform account, assembled from two sources: Keycloak owns the identity
 * (username, email, name, whether login is enabled) and this database owns
 * {@code status} plus which role profiles exist.
 *
 * <p>{@code enabled} and {@code status} can disagree — a user disabled directly
 * in the Keycloak console has no local suspension — so both are reported rather
 * than collapsed into one flag.
 */
public record AdminUserResponse(
        String keycloakUserId,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean emailVerified,
        Boolean enabled,
        Instant keycloakCreatedAt,
        List<ManageableRole> roles,
        AccountStatus status,
        Boolean hasLocalAccount,
        List<String> profiles
) {
}
