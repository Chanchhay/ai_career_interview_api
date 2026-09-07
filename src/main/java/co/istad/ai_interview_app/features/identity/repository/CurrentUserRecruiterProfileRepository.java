package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrentUserRecruiterProfileRepository extends JpaRepository<RecruiterProfile, UUID> {

    Optional<RecruiterProfile> findByUserAccount_Id(UUID userAccountId);
}
