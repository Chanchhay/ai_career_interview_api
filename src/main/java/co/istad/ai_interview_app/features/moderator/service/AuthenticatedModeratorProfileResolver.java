package co.istad.ai_interview_app.features.moderator.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.moderator.repository.ModeratorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * The moderator profile that a moderation decision is recorded against.
 *
 * <p>Creates one if the caller has none. Whether an account may moderate is
 * decided by the realm role in SecurityConfig, which has already admitted this
 * request by the time the resolver runs — so a missing profile row is a gap in
 * provisioning, not a permission question, and refusing the request over it
 * only produces a confusing 404 on an action the caller is entitled to take.
 *
 * <p>Concretely: an administrator holds an {@code AdminProfile} and reaches
 * every moderator endpoint through the role hierarchy, but owned no
 * {@code ModeratorProfile} to attribute a decision to. That combination made
 * approving a candidate or verifying a company impossible for exactly the
 * account meant to be able to do everything.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticatedModeratorProfileResolver {

    private final ModeratorProfileRepository moderatorProfileRepository;
    private final IdentityUserAccountRepository userAccountRepository;

    public ModeratorProfile resolve() {
        String keycloakUserId = AuthUtils.extractUserId();

        return moderatorProfileRepository
                .findByUserAccount_KeycloakUserId(keycloakUserId)
                .orElseGet(() -> provision(keycloakUserId));
    }

    /**
     * Runs inside the caller's transaction, so the profile and the decision it
     * is attributed to commit together or not at all.
     */
    private ModeratorProfile provision(String keycloakUserId) {
        UserAccount userAccount = userAccountRepository
                .findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account was not found for authenticated user"
                ));

        log.info("Creating a moderator profile for user account {}", userAccount.getId());

        ModeratorProfile profile = new ModeratorProfile();
        profile.setUserAccount(userAccount);

        return moderatorProfileRepository.save(profile);
    }
}
