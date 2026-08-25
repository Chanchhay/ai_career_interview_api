package co.istad.ai_interview_app.features.communication.repository;

import co.istad.ai_interview_app.features.communication.entity.ConversationParticipant;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    Optional<ConversationParticipant> findByConversation_IdAndUserAccount_Id(
            Long conversationId,
            Long userAccountId
    );

    List<ConversationParticipant> findAllByConversation_Id(Long conversationId);

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
            @Param("conversationId") Long conversationId,
            @Param("senderUserAccountId") Long senderUserAccountId
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
    List<Long> findSharedOpenConversationIds(
            @Param("firstUserAccountId") Long firstUserAccountId,
            @Param("secondUserAccountId") Long secondUserAccountId,
            @Param("type") ConversationType type
    );
}
