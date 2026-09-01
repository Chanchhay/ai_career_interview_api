package co.istad.ai_interview_app.features.interview.ai.repository;

import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface AiInterviewSessionRepository extends JpaRepository<AiInterviewSession, Long> {

    List<AiInterviewSession> findAllByJobSeeker_KeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    Optional<AiInterviewSession> findByIdAndJobSeeker_KeycloakUserId(Long id, String keycloakUserId);

    Optional<AiInterviewSession> findWithQuestionsByIdAndJobSeeker_KeycloakUserId(Long id, String keycloakUserId);

    Optional<AiInterviewSession> findWithResultByIdAndJobSeeker_KeycloakUserId(Long id, String keycloakUserId);

    /**
     * Resolves a session from a Vapi call id.
     *
     * <p>Unlike every other lookup here it takes no seeker: the caller is Vapi's
     * webhook, which holds a call id and no identity of ours. The call id is
     * itself the authorization — it is unguessable and is only ever bound by the
     * authenticated seeker who owns the session.
     */
    Optional<AiInterviewSession> findWithQuestionsByCallSessionId(String callSessionId);

    Optional<AiInterviewSession> findWithResultByCallSessionId(String callSessionId);

    boolean existsByCallSessionIdAndIdNot(String callSessionId, Long id);

    boolean existsByApplication_IdAndStatusIn(Long applicationId, Collection<co.istad.ai_interview_app.shared.enums.interview.InterviewStatus> statuses);

    /**
     * Sessions this seeker ran against this job that are not attached to an
     * application yet.
     *
     * <p>Used to adopt practice interviews once the candidate applies: the
     * interview they sat is the same interview either way, and which button
     * they happened to press should not decide whether a moderator can see it.
     */
    List<AiInterviewSession> findAllByJobPost_IdAndJobSeeker_IdAndApplicationIsNull(
            Long jobPostId,
            Long jobSeekerUserAccountId
    );

    /** Every session on an application, whatever state it reached. */
    List<AiInterviewSession> findAllByApplication_Id(Long applicationId);

    Optional<AiInterviewSession> findFirstByApplication_IdAndStatusOrderByEndedAtDesc(
            Long applicationId,
            co.istad.ai_interview_app.shared.enums.interview.InterviewStatus status
    );
}
