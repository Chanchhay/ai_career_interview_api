package co.istad.ai_interview_app.features.moderator.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.moderator.repository.ModeratorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuthenticatedModeratorProfileResolver {

    private final ModeratorProfileRepository moderatorProfileRepository;

    public ModeratorProfile resolve() {
        return moderatorProfileRepository.findByUserAccount_KeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Moderator profile was not found for authenticated user"
                ));
    }
}
