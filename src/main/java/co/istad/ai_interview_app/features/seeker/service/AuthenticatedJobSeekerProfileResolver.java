package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.repository.JobSeekerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuthenticatedJobSeekerProfileResolver {

    private final JobSeekerProfileRepository jobSeekerProfileRepository;

    public JobSeekerProfile resolve() {
        return jobSeekerProfileRepository.findByUserAccount_KeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job seeker profile was not found for authenticated user"
                ));
    }
}
