package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface CurrentUserRecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {

    Optional<RecruiterProfile> findByUserAccount_Id(Long userAccountId);
}
