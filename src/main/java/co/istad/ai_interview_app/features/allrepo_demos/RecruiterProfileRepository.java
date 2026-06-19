package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job_recruiter.domain.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {

    Optional<RecruiterProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);
}