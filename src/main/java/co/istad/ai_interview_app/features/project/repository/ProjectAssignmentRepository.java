package co.istad.ai_interview_app.features.project.repository;

import co.istad.ai_interview_app.features.project.entity.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, UUID> {

    List<ProjectAssignment> findAllByApplication_IdOrderByCreatedAtDesc(UUID applicationId);
}
