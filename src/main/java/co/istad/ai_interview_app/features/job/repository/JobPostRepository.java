package co.istad.ai_interview_app.features.job.repository;

import co.istad.ai_interview_app.features.job.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    List<JobPost> findAllByRecruiterProfile_UserAccount_KeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    Optional<JobPost> findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(Long id, String keycloakUserId);
}
