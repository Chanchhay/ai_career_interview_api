package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.job.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
}
