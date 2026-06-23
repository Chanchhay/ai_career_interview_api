package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.project.entity.ProjectReview;
import co.istad.ai_interview_app.shared.enums.InterviewResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface ProjectReviewRepository extends JpaRepository<ProjectReview, Long> {

    List<ProjectReview> findByProjectSubmission_Id(Long projectSubmissionId);

    List<ProjectReview> findByReviewerUserAccount_Id(Long reviewerUserAccountId);

    List<ProjectReview> findByDecision(InterviewResult decision);
}