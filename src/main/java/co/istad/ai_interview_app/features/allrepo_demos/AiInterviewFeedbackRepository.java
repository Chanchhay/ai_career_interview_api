package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job_seeker.domain.ai.AiInterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiInterviewFeedbackRepository extends JpaRepository<AiInterviewFeedback, Long> {
}
