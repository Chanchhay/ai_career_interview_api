package co.istad.ai_interview_app.features.recruiter.repository;

import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {

    Optional<RecruiterProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);
}
