package co.istad.ai_interview_app.features.identity.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        JwtAuthenticationToken jwtAuthentication = AuthUtils.extractJwtAuthentication();
        Jwt jwt = jwtAuthentication.getToken();
        String keycloakUserId = AuthUtils.extractRequiredSubject(jwt);

        log.info("jwt subject: {}", keycloakUserId);

        UserAccount userAccount = userAccountRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Authenticated user is not registered in the application"
                ));

        Long userAccountId = userAccount.getId();

        return new CurrentUserResponse(
                userAccountId,
                keycloakUserId,
                AuthUtils.claimAsString(jwt, "preferred_username"),
                AuthUtils.claimAsString(jwt, "email"),
                AuthUtils.claimAsString(jwt, "given_name"),
                AuthUtils.claimAsString(jwt, "family_name"),
                AuthUtils.resolveFullName(jwt),
                AuthUtils.claimAsString(jwt, "gender"),
                AuthUtils.claimAsString(jwt, "phone_number"),
                AuthUtils.claimAsString(jwt, "registration_source"),
                AuthUtils.resolveRoles(jwtAuthentication.getAuthorities()),
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
}
