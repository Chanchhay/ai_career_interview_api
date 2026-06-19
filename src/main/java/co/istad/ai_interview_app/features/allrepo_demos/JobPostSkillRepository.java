package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job.domain.JobPostSkill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostSkillRepository extends JpaRepository<JobPostSkill, Long> {
}
