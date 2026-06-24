package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrentUserJobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, Long> {

    Optional<JobSeekerProfile> findByUserAccount_Id(Long userAccountId);
}
