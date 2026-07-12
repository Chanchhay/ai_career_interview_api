package co.istad.ai_interview_app.features.interview.ai.repository;

import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiInterviewFeedbackRepository extends JpaRepository<AiInterviewFeedback, Long> {

    Optional<AiInterviewFeedback> findBySession_Id(Long sessionId);
}
