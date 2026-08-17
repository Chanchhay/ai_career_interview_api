package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, Long>, JpaSpecificationExecutor<JobSeekerProfile> {

    Optional<JobSeekerProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);

    Optional<JobSeekerProfile> findByPublicProfileSlugAndStatusAndProfileVisibility(
            String publicProfileSlug,
            ProfileStatus status,
            VisibilityStatus profileVisibility
    );


}
