package co.istad.ai_interview_app.features.application.repository;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobPost_IdAndJobSeekerProfile_Id(Long jobPostId, Long jobSeekerProfileId);

    boolean existsByResume_Id(Long resumeId);

    @EntityGraph(attributePaths = {"jobPost", "resume", "jobSeekerProfile", "jobSeekerProfile.userAccount"})
    List<JobApplication> findAllByJobSeekerProfile_UserAccount_KeycloakUserIdOrderByAppliedAtDesc(String keycloakUserId);

    @EntityGraph(attributePaths = {"jobPost", "resume", "jobSeekerProfile", "jobSeekerProfile.userAccount"})
    Optional<JobApplication> findByIdAndJobSeekerProfile_UserAccount_KeycloakUserId(Long id, String keycloakUserId);

    @EntityGraph(attributePaths = {"jobPost", "resume", "jobSeekerProfile", "jobSeekerProfile.userAccount"})
    Optional<JobApplication> findByIdAndJobPost_RecruiterProfile_UserAccount_KeycloakUserId(Long id, String keycloakUserId);

    @EntityGraph(attributePaths = {"jobPost", "resume", "jobSeekerProfile", "jobSeekerProfile.userAccount"})
    List<JobApplication> findAllByStatus(ApplicationStatus status);
}
