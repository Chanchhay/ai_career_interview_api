package co.istad.ai_interview_app.shared.enums.account;

/**
 * The realm roles an administrator may grant or revoke through the API.
 *
 * <p>Spelled exactly as the Keycloak realm spells them — the admin client looks
 * roles up by this name. Constraining the surface to an enum is the point: it
 * stops a request body from naming an arbitrary realm role, and it keeps the
 * set that user management can hand out visible in one place.
 */
public enum ManageableRole {
    SEEKER,
    RECRUITER,
    MODERATOR,
    FINANCE,
    SUPER_ADMIN
}
