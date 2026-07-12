package co.istad.ai_interview_app.features.interview.ai.repository;

import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiInterviewSessionRepository extends JpaRepository<AiInterviewSession, Long> {

    List<AiInterviewSession> findAllByJobSeeker_KeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    Optional<AiInterviewSession> findByIdAndJobSeeker_KeycloakUserId(Long id, String keycloakUserId);

    Optional<AiInterviewSession> findWithQuestionsByIdAndJobSeeker_KeycloakUserId(Long id, String keycloakUserId);

    Optional<AiInterviewSession> findWithResultByIdAndJobSeeker_KeycloakUserId(Long id, String keycloakUserId);
}
