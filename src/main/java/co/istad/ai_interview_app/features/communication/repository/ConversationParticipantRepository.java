package co.istad.ai_interview_app.features.communication.repository;

import co.istad.ai_interview_app.features.communication.entity.ConversationParticipant;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    Optional<ConversationParticipant> findByConversation_IdAndUserAccount_Id(
            UUID conversationId,
            UUID userAccountId
    );

    List<ConversationParticipant> findAllByConversation_Id(UUID conversationId);

    /**
     * Everyone still in the thread except the sender — the set a new message
     * should notify.
     */
    @Query("""
            select participant
            from ConversationParticipant participant
            where participant.conversation.id = :conversationId
              and participant.userAccount.id <> :senderUserAccountId
              and participant.leftAt is null
            """)
    List<ConversationParticipant> findRecipients(
            @Param("conversationId") UUID conversationId,
            @Param("senderUserAccountId") UUID senderUserAccountId
    );

    /**
     * Whether an open thread already links these two accounts, so repeated
     * "message this recruiter" clicks reuse one thread.
     */
    @Query("""
            select participant.conversation.id
            from ConversationParticipant participant
            where participant.userAccount.id in (:firstUserAccountId, :secondUserAccountId)
              and participant.leftAt is null
              and participant.conversation.type = :type
              and participant.conversation.status = co.istad.ai_interview_app.shared.enums.conversation.ConversationStatus.OPEN
            group by participant.conversation.id
            having count(distinct participant.userAccount.id) = 2
            """)
    List<UUID> findSharedOpenConversationIds(
            @Param("firstUserAccountId") UUID firstUserAccountId,
            @Param("secondUserAccountId") UUID secondUserAccountId,
            @Param("type") ConversationType type
    );
}
