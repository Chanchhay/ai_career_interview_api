package co.istad.ai_interview_app.features.admin.users.service;

import co.istad.ai_interview_app.config.props.KeycloakAdminClientProps;
import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.admin.audit.service.AuditLogRecorder;
import co.istad.ai_interview_app.features.admin.entity.AdminProfile;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserCreateRequest;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserResponse;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserRolesRequest;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserStatusRequest;
import co.istad.ai_interview_app.features.finance.entity.FinanceProfile;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserAdminProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserFinanceProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserJobSeekerProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserModeratorProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserRecruiterProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.account.AccountStatus;
import co.istad.ai_interview_app.shared.enums.account.ManageableRole;
import co.istad.ai_interview_app.shared.enums.admin.AuditActionType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

/**
 * Administrative user management across the two systems that together make up
 * an account: Keycloak holds the identity and the realm roles, this database
 * holds {@code user_accounts.status} and the per-role profile rows.
 *
 * <p>Every write touches Keycloak first and the database second. Keycloak is
 * the one that decides whether a login succeeds, so if the second step fails
 * the account is already locked out — the safe direction for a suspension to
 * fail. The reverse order would leave a suspended-looking record that can still
 * sign in.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final String AUDIT_ENTITY_NAME = "UserAccount";

    private final Keycloak keycloak;
    private final KeycloakAdminClientProps props;
    private final IdentityUserAccountRepository userAccountRepository;
    private final CurrentUserJobSeekerProfileRepository jobSeekerProfileRepository;
    private final CurrentUserRecruiterProfileRepository recruiterProfileRepository;
    private final CurrentUserModeratorProfileRepository moderatorProfileRepository;
    private final CurrentUserAdminProfileRepository adminProfileRepository;
    private final CurrentUserFinanceProfileRepository financeProfileRepository;
    private final AuditLogRecorder auditLogRecorder;

    /**
     * How many Keycloak users a single list request will pull before filtering
     * and paging in memory.
     *
     * <p>Filtering by role and by local status cannot be expressed in one
     * Keycloak query, so the candidate set is fetched, joined against this
     * database, and only then paged. That is correct but bounded: past this
     * many matching users the tail is not reachable through the list, and the
     * search box becomes the way to find someone. Raise it, or move the user
     * directory into this database, if the realm outgrows it.
     */
    @Value("${app.admin.users.max-scan:500}")
    private int maxScan;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> findUsers(
            String search,
            ManageableRole role,
            AccountStatus status,
            Pageable pageable
    ) {
        List<UserRepresentation> candidates = role == null
                ? searchUsers(search)
                : filterBySearchTerm(roleMembers(role), search);

        Map<String, UserAccount> localAccounts = loadLocalAccounts(candidates);

        List<UserRepresentation> matching = candidates.stream()
                .filter(user -> matchesStatus(user, localAccounts, status))
                .sorted(Comparator.comparing(
                        UserRepresentation::getUsername,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                ))
                .toList();

        int from = (int) Math.min(pageable.getOffset(), matching.size());
        int to = Math.min(from + pageable.getPageSize(), matching.size());
        List<UserRepresentation> slice = matching.subList(from, to);

        // Roles are read per user, so they are read only for the rows actually
        // being returned rather than for every candidate.
        List<AdminUserResponse> content = slice.stream()
                .map(user -> toResponse(user, localAccounts.get(user.getId()), realmRolesOf(user.getId()), false))
                .toList();

        return new PageImpl<>(content, pageable, matching.size());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(String keycloakUserId) {
        UserRepresentation user = requireKeycloakUser(keycloakUserId);
        UserAccount localAccount = userAccountRepository.findByKeycloakUserId(keycloakUserId).orElse(null);

        return toResponse(user, localAccount, realmRolesOf(keycloakUserId), true);
    }

    @Override
    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        Set<ManageableRole> roles = normalizeRoles(request.roles());

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(request.username());
        userRepresentation.setEmail(request.email());
        userRepresentation.setFirstName(request.firstName());
        userRepresentation.setLastName(request.lastName());
        userRepresentation.setEnabled(true);
        // Staff accounts are created by a person who already knows the address,
        // so the address counts as verified and the invitee is not blocked by a
        // confirmation mail that may never arrive.
        userRepresentation.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(Boolean.TRUE.equals(request.temporaryPassword()));
        userRepresentation.setCredentials(List.of(credential));

        UsersResource usersResource = users();
        String createdUserId;

        try (Response response = usersResource.create(userRepresentation)) {
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A user with the same username or email already exists"
                );
            }

            if (response.getStatus() != HttpStatus.CREATED.value()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Keycloak user creation failed with status " + response.getStatus()
                );
            }

            createdUserId = CreatedResponseUtil.getCreatedId(response);
        }

        grantRoles(createdUserId, roles);

        UserAccount userAccount = new UserAccount();
        userAccount.setKeycloakUserId(createdUserId);
        userAccount.setStatus(AccountStatus.ACTIVE);
        UserAccount savedAccount = userAccountRepository.save(userAccount);
        ensureProfiles(savedAccount, roles);

        auditLogRecorder.record(
                AuditActionType.CREATE,
                AUDIT_ENTITY_NAME,
                createdUserId,
                "Administrator created account %s".formatted(request.username()),
                null,
                Map.of(
                        "username", request.username(),
                        "email", request.email(),
                        "roles", roles.stream().map(Enum::name).toList()
                )
        );

        return getUser(createdUserId);
    }

    @Override
    @Transactional
    public AdminUserResponse suspendUser(String keycloakUserId, AdminUserStatusRequest request) {
        return changeStatus(keycloakUserId, AccountStatus.SUSPENDED, request);
    }

    @Override
    @Transactional
    public AdminUserResponse reactivateUser(String keycloakUserId, AdminUserStatusRequest request) {
        return changeStatus(keycloakUserId, AccountStatus.ACTIVE, request);
    }

    @Override
    @Transactional
    public AdminUserResponse updateRoles(String keycloakUserId, AdminUserRolesRequest request) {
        rejectSelfAction(keycloakUserId, "change your own roles");
        requireKeycloakUser(keycloakUserId);

        Set<ManageableRole> desired = normalizeRoles(request.roles());
        Set<ManageableRole> current = realmRolesOf(keycloakUserId);

        Set<ManageableRole> toGrant = difference(desired, current);
        Set<ManageableRole> toRevoke = difference(current, desired);

        if (toGrant.isEmpty() && toRevoke.isEmpty()) {
            return getUser(keycloakUserId);
        }

        grantRoles(keycloakUserId, toGrant);
        revokeRoles(keycloakUserId, toRevoke);

        /*
         * Newly granted roles get their profile row; revoked roles keep theirs.
         * A moderator profile carries review history and a recruiter profile
         * owns companies and jobs — deleting either to mirror a role change
         * would destroy records the platform still references. The role is what
         * grants access; the profile is only data.
         */
        userAccountRepository.findByKeycloakUserId(keycloakUserId)
                .ifPresent(account -> ensureProfiles(account, toGrant));

        auditLogRecorder.record(
                AuditActionType.UPDATE,
                AUDIT_ENTITY_NAME,
                keycloakUserId,
                "Administrator changed realm roles",
                Map.of("roles", current.stream().map(Enum::name).sorted().toList()),
                Map.of("roles", desired.stream().map(Enum::name).sorted().toList())
        );

        return getUser(keycloakUserId);
    }

    /* ------------------------------------------------------------ status --- */

    private AdminUserResponse changeStatus(
            String keycloakUserId,
            AccountStatus target,
            AdminUserStatusRequest request
    ) {
        rejectSelfAction(
                keycloakUserId,
                target == AccountStatus.SUSPENDED ? "suspend your own account" : "reactivate your own account"
        );

        UserRepresentation user = requireKeycloakUser(keycloakUserId);
        boolean enabled = target == AccountStatus.ACTIVE;

        user.setEnabled(enabled);
        users().get(keycloakUserId).update(user);

        UserAccount account = userAccountRepository.findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> {
                    // A Keycloak-only account, typically one created in the
                    // console. Give it the local row now so the per-request
                    // status check has something to read.
                    UserAccount created = new UserAccount();
                    created.setKeycloakUserId(keycloakUserId);
                    return created;
                });

        AccountStatus previous = account.getStatus();
        account.setStatus(target);
        userAccountRepository.save(account);

        auditLogRecorder.record(
                target == AccountStatus.SUSPENDED ? AuditActionType.SUSPEND : AuditActionType.ACTIVATE,
                AUDIT_ENTITY_NAME,
                keycloakUserId,
                hasText(request == null ? null : request.reason())
                        ? request.reason()
                        : "Administrator set account status to " + target,
                Map.of("status", String.valueOf(previous), "enabled", !enabled),
                Map.of("status", target.name(), "enabled", enabled)
        );

        return getUser(keycloakUserId);
    }

    /**
     * An administrator may not suspend themselves or edit their own roles.
     *
     * <p>Not paternalism: SUPER_ADMIN is the only role that can undo either
     * action, so a self-inflicted one can leave the platform with nobody able
     * to reverse it.
     */
    private void rejectSelfAction(String keycloakUserId, String action) {
        if (keycloakUserId.equals(AuthUtils.extractUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You cannot " + action
            );
        }
    }

    /* ------------------------------------------------------------- roles --- */

    private Set<ManageableRole> normalizeRoles(List<ManageableRole> roles) {
        Set<ManageableRole> normalized = roles == null || roles.isEmpty()
                ? EnumSet.noneOf(ManageableRole.class)
                : EnumSet.copyOf(roles);

        if (normalized.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "An account must keep at least one role"
            );
        }

        return normalized;
    }

    private Set<ManageableRole> difference(Set<ManageableRole> from, Set<ManageableRole> remove) {
        Set<ManageableRole> result = EnumSet.noneOf(ManageableRole.class);
        from.stream().filter(role -> !remove.contains(role)).forEach(result::add);
        return result;
    }

    private void grantRoles(String keycloakUserId, Set<ManageableRole> roles) {
        if (roles.isEmpty()) return;

        users().get(keycloakUserId).roles().realmLevel().add(toRepresentations(roles));
    }

    private void revokeRoles(String keycloakUserId, Set<ManageableRole> roles) {
        if (roles.isEmpty()) return;

        users().get(keycloakUserId).roles().realmLevel().remove(toRepresentations(roles));
    }

    private List<RoleRepresentation> toRepresentations(Set<ManageableRole> roles) {
        RealmResource realm = realm();

        return roles.stream()
                .map(role -> {
                    try {
                        return realm.roles().get(role.name()).toRepresentation();
                    } catch (RuntimeException exception) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                "Realm role %s is missing from Keycloak".formatted(role.name()),
                                exception
                        );
                    }
                })
                .toList();
    }

    /**
     * The caller's realm roles, narrowed to the ones this API manages. Roles the
     * realm defines for other purposes are ignored rather than reported, so a
     * role update cannot accidentally strip them.
     */
    private Set<ManageableRole> realmRolesOf(String keycloakUserId) {
        Set<ManageableRole> roles = EnumSet.noneOf(ManageableRole.class);

        users().get(keycloakUserId).roles().realmLevel().listEffective().forEach(role -> {
            for (ManageableRole manageable : ManageableRole.values()) {
                if (manageable.name().equalsIgnoreCase(role.getName())) {
                    roles.add(manageable);
                }
            }
        });

        return roles;
    }

    private List<UserRepresentation> roleMembers(ManageableRole role) {
        try {
            return new ArrayList<>(realm().roles().get(role.name()).getUserMembers(0, maxScan));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not read members of realm role " + role.name(),
                    exception
            );
        }
    }

    /* ----------------------------------------------------------- lookups --- */

    private List<UserRepresentation> searchUsers(String search) {
        return hasText(search)
                ? users().search(search.trim(), 0, maxScan)
                : users().list(0, maxScan);
    }

    private List<UserRepresentation> filterBySearchTerm(List<UserRepresentation> users, String search) {
        if (!hasText(search)) return users;

        String term = search.trim().toLowerCase();

        return users.stream()
                .filter(user -> containsIgnoreCase(user.getUsername(), term)
                        || containsIgnoreCase(user.getEmail(), term)
                        || containsIgnoreCase(user.getFirstName(), term)
                        || containsIgnoreCase(user.getLastName(), term))
                .toList();
    }

    private boolean containsIgnoreCase(String value, String lowercaseTerm) {
        return value != null && value.toLowerCase().contains(lowercaseTerm);
    }

    private Map<String, UserAccount> loadLocalAccounts(List<UserRepresentation> users) {
        if (users.isEmpty()) return Map.of();

        List<String> ids = users.stream().map(UserRepresentation::getId).filter(java.util.Objects::nonNull).toList();

        return userAccountRepository.findAllByKeycloakUserIdIn(ids).stream()
                .collect(Collectors.toMap(
                        UserAccount::getKeycloakUserId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }

    /**
     * A user with no local row counts as ACTIVE for filtering: nothing has ever
     * suspended it, which is what the filter question is really asking.
     */
    private boolean matchesStatus(
            UserRepresentation user,
            Map<String, UserAccount> localAccounts,
            AccountStatus status
    ) {
        if (status == null) return true;

        UserAccount account = localAccounts.get(user.getId());
        AccountStatus effective = account == null ? AccountStatus.ACTIVE : account.getStatus();

        return effective == status;
    }

    private UserRepresentation requireKeycloakUser(String keycloakUserId) {
        try {
            UserResource userResource = users().get(keycloakUserId);
            return userResource.toRepresentation();
        } catch (jakarta.ws.rs.NotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found");
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not read the user from Keycloak",
                    exception
            );
        }
    }

    /* ---------------------------------------------------------- profiles --- */

    private void ensureProfiles(UserAccount account, Set<ManageableRole> roles) {
        for (ManageableRole role : roles) {
            switch (role) {
                case SEEKER -> jobSeekerProfileRepository.findByUserAccount_Id(account.getId())
                        .orElseGet(() -> {
                            JobSeekerProfile profile = new JobSeekerProfile();
                            profile.setUserAccount(account);
                            return jobSeekerProfileRepository.save(profile);
                        });
                case RECRUITER -> recruiterProfileRepository.findByUserAccount_Id(account.getId())
                        .orElseGet(() -> {
                            RecruiterProfile profile = new RecruiterProfile();
                            profile.setUserAccount(account);
                            return recruiterProfileRepository.save(profile);
                        });
                case MODERATOR -> moderatorProfileRepository.findByUserAccount_Id(account.getId())
                        .orElseGet(() -> {
                            ModeratorProfile profile = new ModeratorProfile();
                            profile.setUserAccount(account);
                            return moderatorProfileRepository.save(profile);
                        });
                case FINANCE -> financeProfileRepository.findByUserAccount_Id(account.getId())
                        .orElseGet(() -> {
                            FinanceProfile profile = new FinanceProfile();
                            profile.setUserAccount(account);
                            return financeProfileRepository.save(profile);
                        });
                case SUPER_ADMIN -> adminProfileRepository.findByUserAccount_Id(account.getId())
                        .orElseGet(() -> {
                            AdminProfile profile = new AdminProfile();
                            profile.setUserAccount(account);
                            return adminProfileRepository.save(profile);
                        });
            }
        }
    }

    private List<String> profileNames(UserAccount account) {
        if (account == null) return List.of();

        List<String> names = new ArrayList<>();
        Long id = account.getId();

        jobSeekerProfileRepository.findByUserAccount_Id(id).ifPresent(profile -> names.add("JOB_SEEKER"));
        recruiterProfileRepository.findByUserAccount_Id(id).ifPresent(profile -> names.add("RECRUITER"));
        moderatorProfileRepository.findByUserAccount_Id(id).ifPresent(profile -> names.add("MODERATOR"));
        financeProfileRepository.findByUserAccount_Id(id).ifPresent(profile -> names.add("FINANCE"));
        adminProfileRepository.findByUserAccount_Id(id).ifPresent(profile -> names.add("ADMIN"));

        return names;
    }

    /* ---------------------------------------------------------- mapping --- */

    /**
     * @param includeProfiles the list view leaves {@code profiles} empty — five
     *                        extra queries per row is not worth a column nobody
     *                        reads until they open the user.
     */
    private AdminUserResponse toResponse(
            UserRepresentation user,
            UserAccount localAccount,
            Set<ManageableRole> roles,
            boolean includeProfiles
    ) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEmailVerified(),
                user.isEnabled(),
                user.getCreatedTimestamp() == null ? null : Instant.ofEpochMilli(user.getCreatedTimestamp()),
                roles.stream().sorted().toList(),
                localAccount == null ? AccountStatus.ACTIVE : localAccount.getStatus(),
                localAccount != null,
                includeProfiles ? profileNames(localAccount) : List.of()
        );
    }

    private RealmResource realm() {
        return keycloak.realm(props.getTargetRealm());
    }

    private UsersResource users() {
        return realm().users();
    }
}
