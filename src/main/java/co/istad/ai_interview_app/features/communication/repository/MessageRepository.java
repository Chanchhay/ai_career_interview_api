package co.istad.ai_interview_app.features.communication.repository;

import co.istad.ai_interview_app.features.communication.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findAllByConversation_IdOrderBySentAtDesc(Long conversationId, Pageable pageable);

    Optional<Message> findByIdAndConversation_Id(Long id, Long conversationId);

    /**
     * The newest message in each of the given threads, as one query rather than
     * one per row of the inbox.
     *
     * <p>Two messages sharing the exact same instant would both come back; the
     * caller keeps one. Cheaper than a window function and correct either way.
     */
    @Query("""
            select message
            from Message message
            where message.conversation.id in :conversationIds
              and message.sentAt = (
                  select max(latest.sentAt)
                  from Message latest
                  where latest.conversation.id = message.conversation.id
              )
            """)
    List<Message> findLatestPerConversation(
            @Param("conversationIds") Collection<Long> conversationIds
    );

    /**
     * Unread counts for the whole inbox in one query.
     *
     * <p>Unread means "sent by someone else, after my {@code lastReadAt}".
     * Read state is tracked per participant, not per message: a thread can hold
     * more than two people, and a single {@code Message.readAt} could not say
     * which of them had seen it.
     *
     * @return rows of {@code [conversationId, count]}
     */
    @Query("""
            select message.conversation.id, count(message)
            from Message message, ConversationParticipant participant
            where participant.conversation.id = message.conversation.id
              and participant.userAccount.id = :userAccountId
              and message.conversation.id in :conversationIds
              and message.senderUserAccount.id <> :userAccountId
              and message.status <> co.istad.ai_interview_app.shared.enums.conversation.MessageStatus.DELETED
              and (participant.lastReadAt is null or message.sentAt > participant.lastReadAt)
            group by message.conversation.id
            """)
    List<Object[]> countUnreadPerConversation(
            @Param("userAccountId") Long userAccountId,
            @Param("conversationIds") Collection<Long> conversationIds
    );
}
