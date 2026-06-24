package co.istad.ai_interview_app.features.identity.service;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.identity.dto.CurrentUserProfilesResponse;
import co.istad.ai_interview_app.features.identity.dto.CurrentUserResponse;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserAdminProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserFinanceProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserJobSeekerProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserModeratorProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserRecruiterProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final IdentityUserAccountRepository userAccountRepository;
    private final CurrentUserJobSeekerProfileRepository jobSeekerProfileRepository;
    private final CurrentUserRecruiterProfileRepository recruiterProfileRepository;
    private final CurrentUserModeratorProfileRepository moderatorProfileRepository;
    private final CurrentUserAdminProfileRepository adminProfileRepository;
    private final CurrentUserFinanceProfileRepository financeProfileRepository;

    @Override
    @Transactional
    public CurrentUserResponse getCurrentUser(
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities
    ) {
        String keycloakUserId = jwt.getSubject();
        log.info("jwt: {}", jwt);
        log.info("subject: {}", keycloakUserId);

        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated token is missing subject");
        }

        UserAccount userAccount = userAccountRepository.findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> createUserAccount(keycloakUserId, authorities));

        Long userAccountId = userAccount.getId();

        return new CurrentUserResponse(
                userAccountId,
                keycloakUserId,
                claimAsString(jwt, "email"),
                resolveFullName(jwt),
                resolveRoles(authorities),
                new CurrentUserProfilesResponse(
                        jobSeekerProfileRepository.findByUserAccount_Id(userAccountId)
                                .map(BaseEntity::getId)
                                .orElse(null),
                        recruiterProfileRepository.findByUserAccount_Id(userAccountId)
                                .map(BaseEntity::getId)
                                .orElse(null),
                        moderatorProfileRepository.findByUserAccount_Id(userAccountId)
                                .map(BaseEntity::getId)
                                .orElse(null),
                        adminProfileRepository.findByUserAccount_Id(userAccountId)
                                .map(BaseEntity::getId)
                                .orElse(null),
                        financeProfileRepository.findByUserAccount_Id(userAccountId)
                                .map(BaseEntity::getId)
                                .orElse(null)
                )
        );
    }

    private UserAccount createUserAccount(
            String keycloakUserId,
            Collection<? extends GrantedAuthority> authorities
    ) {
        RegistrationRole registrationRole = resolveRegistrationRole(authorities);

        UserAccount userAccount = new UserAccount();
        userAccount.setKeycloakUserId(keycloakUserId);
        UserAccount savedUserAccount = userAccountRepository.save(userAccount);

        if (registrationRole == RegistrationRole.SEEKER) {
            JobSeekerProfile jobSeekerProfile = new JobSeekerProfile();
            jobSeekerProfile.setUserAccount(savedUserAccount);
            jobSeekerProfileRepository.save(jobSeekerProfile);
        } else {
            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(savedUserAccount);
            recruiterProfileRepository.save(recruiterProfile);
        }

        return savedUserAccount;
    }

    private RegistrationRole resolveRegistrationRole(Collection<? extends GrantedAuthority> authorities) {
        boolean hasSeekerRole = hasRole(authorities, "SEEKER");
        boolean hasRecruiterRole = hasRole(authorities, "RECRUITER");

        if (hasSeekerRole && hasRecruiterRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated user cannot create both seeker and recruiter profiles"
            );
        }

        if (hasSeekerRole) {
            return RegistrationRole.SEEKER;
        }

        if (hasRecruiterRole) {
            return RegistrationRole.RECRUITER;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Authenticated user must have SEEKER or RECRUITER role to create account"
        );
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
        String roleAuthority = "ROLE_" + role;
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(authority -> authority.equalsIgnoreCase(roleAuthority) || authority.equalsIgnoreCase(role));
    }

    private List<String> resolveRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    private String resolveFullName(Jwt jwt) {
        String fullName = claimAsString(jwt, "name");

        if (hasText(fullName)) {
            return fullName;
        }

        String givenName = claimAsString(jwt, "given_name");
        String familyName = claimAsString(jwt, "family_name");
        String combinedName = Stream.of(givenName, familyName)
                .filter(this::hasText)
                .collect(Collectors.joining(" "));

        if (hasText(combinedName)) {
            return combinedName;
        }

        String preferredUsername = claimAsString(jwt, "preferred_username");
        if (hasText(preferredUsername)) {
            return preferredUsername;
        }

        return claimAsString(jwt, "email");
    }

    private String claimAsString(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        return value == null ? null : value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private enum RegistrationRole {
        SEEKER,
        RECRUITER
    }
}
