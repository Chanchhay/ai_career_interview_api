package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.interview.ai.entity.AiInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiInterviewSessionRepository extends JpaRepository<AiInterviewSession, Long> {
}
