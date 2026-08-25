package co.istad.ai_interview_app.features.interview.human.repository;

import co.istad.ai_interview_app.features.interview.human.entity.HumanInterview;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HumanInterviewRepository extends JpaRepository<HumanInterview, Long> {

    List<HumanInterview> findAllByApplication_IdOrderByScheduledAtDesc(Long applicationId);

    boolean existsByApplication_IdAndStatus(Long applicationId, InterviewStatus status);

    /**
     * Whether a booked interview is still waiting to happen.
     *
     * <p>Completed and cancelled both count as settled: a cancelled interview is
     * one the moderator decided not to hold, so it should stop blocking the
     * decision rather than block it forever.
     */
    boolean existsByApplication_IdAndStatusIn(Long applicationId, Collection<InterviewStatus> statuses);

    @EntityGraph(attributePaths = {"application", "application.jobPost", "moderator"})
    Optional<HumanInterview> findByIdAndModerator_UserAccount_KeycloakUserId(Long id, String keycloakUserId);
}
