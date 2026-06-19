package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.moderator.domain.ProjectAssignment;
import co.istad.ai_interview_app.shared.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    List<ProjectAssignment> findByApplication_Id(Long applicationId);

    List<ProjectAssignment> findByAssignedByModeratorProfile_Id(Long moderatorProfileId);

    List<ProjectAssignment> findByStatus(ProjectStatus status);
}