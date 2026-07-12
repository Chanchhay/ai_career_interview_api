package co.istad.ai_interview_app.features.interview.ai.repository;

import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiInterviewQuestionRepository extends JpaRepository<AiInterviewQuestion, Long> {

    List<AiInterviewQuestion> findAllBySession_IdOrderByDisplayOrder(Long sessionId);

    Optional<AiInterviewQuestion> findByIdAndSession_IdAndSession_JobSeeker_KeycloakUserId(
            Long id,
            Long sessionId,
            String keycloakUserId
    );
}
