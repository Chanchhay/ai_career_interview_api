package co.istad.ai_interview_app.features.project.repository;

import co.istad.ai_interview_app.features.project.entity.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    List<ProjectAssignment> findAllByApplication_IdOrderByCreatedAtDesc(Long applicationId);
}
