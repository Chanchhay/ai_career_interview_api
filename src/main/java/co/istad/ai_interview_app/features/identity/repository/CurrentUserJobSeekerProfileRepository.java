package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrentUserJobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, UUID> {

    Optional<JobSeekerProfile> findByUserAccount_Id(UUID userAccountId);
}
