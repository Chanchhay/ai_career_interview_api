package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job_seeker.domain.ProjectSubmission;
import co.istad.ai_interview_app.shared.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface ProjectSubmissionRepository extends JpaRepository<ProjectSubmission, Long> {

    List<ProjectSubmission> findByJobSeekerProfile_Id(Long jobSeekerProfileId);

    List<ProjectSubmission> findByProjectAssignment_Id(Long projectAssignmentId);

    List<ProjectSubmission> findByJobSeekerProfile_IdAndStatus(
            Long jobSeekerProfileId,
            ProjectStatus status
    );

    Optional<ProjectSubmission> findByProjectAssignment_IdAndJobSeekerProfile_Id(
            Long projectAssignmentId,
            Long jobSeekerProfileId
    );

    boolean existsByProjectAssignment_IdAndJobSeekerProfile_Id(
            Long projectAssignmentId,
            Long jobSeekerProfileId
    );
}
