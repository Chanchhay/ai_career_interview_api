package co.istad.ai_interview_app.auth;

import co.istad.ai_interview_app.auth.dto.RegisterRequest;
import co.istad.ai_interview_app.auth.dto.RegisterResponse;
import co.istad.ai_interview_app.auth.mapper.AuthMapper;
import co.istad.ai_interview_app.config.props.KeycloakAdminClientProps;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserJobSeekerProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final Keycloak keycloak;
    private final AuthMapper authMapper;
    private final KeycloakAdminClientProps props;
    private final IdentityUserAccountRepository userAccountRepository;
    private final CurrentUserJobSeekerProfileRepository jobSeekerProfileRepository;

    private static final String ATTRIBUTE_GENDER = "gender";
    private static final String ATTRIBUTE_PHONE_NUMBER = "phone_number";
    private static final String ATTRIBUTE_REGISTRATION_SOURCE = "registration_source";
    private static final String DEFAULT_REGISTRATION_ROLE = "SEEKER";
    private static final String REGISTRATION_SOURCE_SELF = "self_registration";

    @Override
    @Transactional
    public RegisterResponse register(@Valid RegisterRequest registerRequest) {
        if (!registerRequest.password().equals(registerRequest.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password and Confirm password does not match");
        }

        UserRepresentation userRepresentation = authMapper.toUserRepresentation(registerRequest);
        userRepresentation.setAttributes(buildUserAttributes(registerRequest));
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(false);

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType("password");
        credentialRepresentation.setValue(registerRequest.password());
        credentialRepresentation.setTemporary(false);
        userRepresentation.setCredentials(List.of(credentialRepresentation));

        UsersResource usersResource = keycloak.realm(props.getTargetRealm()).users();

        try (Response response = usersResource.create(userRepresentation)) {
            log.info("Response status code: {}", response.getStatus());

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

            String createdUserId = CreatedResponseUtil.getCreatedId(response);
            assignDefaultRegistrationRole(createdUserId);
            UserAccount userAccount = createLocalUserAccount(createdUserId);
            UserRepresentation createdUser = usersResource.get(createdUserId).toRepresentation();

            return RegisterResponse.builder()
                    .id(userAccount.getKeycloakUserId())
                    .username(createdUser.getUsername())
                    .email(createdUser.getEmail())
                    .firstName(createdUser.getFirstName())
                    .lastName(createdUser.getLastName())
                    .gender(readAttribute(createdUser, ATTRIBUTE_GENDER))
                    .phoneNumber(readAttribute(createdUser, ATTRIBUTE_PHONE_NUMBER))
                    .registrationSource(readAttribute(createdUser, ATTRIBUTE_REGISTRATION_SOURCE))
                    .build();
        }
    }

    private Map<String, List<String>> buildUserAttributes(RegisterRequest registerRequest) {
        Map<String, List<String>> attributes = new HashMap<>();

        if (registerRequest.gender() != null) {
            attributes.put(ATTRIBUTE_GENDER, List.of(registerRequest.gender().name()));
        }

        if (hasText(registerRequest.phoneNumber())) {
            attributes.put(ATTRIBUTE_PHONE_NUMBER, List.of(registerRequest.phoneNumber()));
        }

        attributes.put(ATTRIBUTE_REGISTRATION_SOURCE, List.of(REGISTRATION_SOURCE_SELF));

        return attributes;
    }

    private void assignDefaultRegistrationRole(String keycloakUserId) {
        RoleRepresentation role = keycloak.realm(props.getTargetRealm())
                .roles()
                .get(DEFAULT_REGISTRATION_ROLE)
                .toRepresentation();

        keycloak.realm(props.getTargetRealm())
                .users()
                .get(keycloakUserId)
                .roles()
                .realmLevel()
                .add(List.of(role));
    }

    private UserAccount createLocalUserAccount(String keycloakUserId) {
        UserAccount userAccount = new UserAccount();
        userAccount.setKeycloakUserId(keycloakUserId);
        UserAccount savedUserAccount = userAccountRepository.save(userAccount);

        JobSeekerProfile jobSeekerProfile = new JobSeekerProfile();
        jobSeekerProfile.setUserAccount(savedUserAccount);
        jobSeekerProfileRepository.save(jobSeekerProfile);

        return savedUserAccount;
    }

    private String readAttribute(UserRepresentation userRepresentation, String attributeName) {
        return readAtt(userRepresentation, attributeName);
    }

    @Nullable
    public static String readAtt(UserRepresentation userRepresentation, String attributeName) {
        Map<String, List<String>> attributes = userRepresentation.getAttributes();

        if (attributes == null || attributes.get(attributeName) == null || attributes.get(attributeName).isEmpty()) {
            return null;
        }

        return attributes.get(attributeName).getFirst();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
