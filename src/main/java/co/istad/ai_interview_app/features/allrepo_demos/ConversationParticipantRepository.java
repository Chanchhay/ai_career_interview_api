package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.communication.domain.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByConversation_Id(Long conversationId);

    List<ConversationParticipant> findByUserAccount_Id(Long userAccountId);

    Optional<ConversationParticipant> findByConversation_IdAndUserAccount_Id(
            Long conversationId,
            Long userAccountId
    );

    boolean existsByConversation_IdAndUserAccount_Id(
            Long conversationId,
            Long userAccountId
    );
}