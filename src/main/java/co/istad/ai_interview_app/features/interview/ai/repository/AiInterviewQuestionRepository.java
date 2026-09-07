package co.istad.ai_interview_app.features.interview.ai.repository;

import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiInterviewQuestionRepository extends JpaRepository<AiInterviewQuestion, UUID> {

    List<AiInterviewQuestion> findAllBySession_IdOrderByDisplayOrder(UUID sessionId);

    Optional<AiInterviewQuestion> findByIdAndSession_IdAndSession_JobSeeker_KeycloakUserId(
            UUID id,
            UUID sessionId,
            String keycloakUserId
    );
}
