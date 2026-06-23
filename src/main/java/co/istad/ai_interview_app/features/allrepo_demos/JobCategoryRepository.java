package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.job.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {
}
