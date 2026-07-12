package co.istad.ai_interview_app.features.interview.human.repository;

import co.istad.ai_interview_app.features.interview.human.entity.HumanInterview;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HumanInterviewRepository extends JpaRepository<HumanInterview, Long> {

    List<HumanInterview> findAllByApplication_IdOrderByScheduledAtDesc(Long applicationId);

    boolean existsByApplication_IdAndStatus(Long applicationId, InterviewStatus status);

    @EntityGraph(attributePaths = {"application", "application.jobPost", "moderator"})
    Optional<HumanInterview> findByIdAndModerator_UserAccount_KeycloakUserId(Long id, String keycloakUserId);
}
