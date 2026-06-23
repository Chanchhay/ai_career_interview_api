package co.istad.ai_interview_app.features.allrepo_demos;


import co.istad.ai_interview_app.seeker.entity.JobSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource
public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, Long> {

    Optional<JobSeekerProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);
}
