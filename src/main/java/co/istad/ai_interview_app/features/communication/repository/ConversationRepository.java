package co.istad.ai_interview_app.features.communication.repository;

import co.istad.ai_interview_app.features.communication.entity.Conversation;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationStatus;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * The caller's threads, newest activity first.
     *
     * <p>Ordered by the last message rather than by creation: an inbox sorted
     * by when a thread started puts a live conversation below a stale one. The
     * subquery leaves threads with no messages yet at their creation time.
     */
    @Query(
            value = """
                    select conversation
                    from Conversation conversation
                    join conversation.participants participant
                    where participant.userAccount.id = :userAccountId
                      and participant.leftAt is null
                    order by coalesce(
                        (select max(message.sentAt)
                         from Message message
                         where message.conversation.id = conversation.id),
                        conversation.createdAt
                    ) desc
                    """,
            countQuery = """
                    select count(conversation)
                    from Conversation conversation
                    join conversation.participants participant
                    where participant.userAccount.id = :userAccountId
                      and participant.leftAt is null
                    """
    )
    Page<Conversation> findAllForParticipant(
            @Param("userAccountId") Long userAccountId,
            Pageable pageable
    );

    /**
     * The caller's existing open support thread, if any.
     *
     * <p>Support is deliberately one-open-thread-per-person: it keeps a single
     * readable history for whoever picks it up, and it means a frustrated user
     * clicking twice cannot fan out into a queue of near-identical threads.
     */
    @Query("""
            select conversation
            from Conversation conversation
            join conversation.participants participant
            where participant.userAccount.id = :userAccountId
              and participant.leftAt is null
              and conversation.type = co.istad.ai_interview_app.shared.enums.conversation.ConversationType.SUPPORT
              and conversation.status = co.istad.ai_interview_app.shared.enums.conversation.ConversationStatus.OPEN
            order by conversation.id desc
            """)
    List<Conversation> findOpenSupportConversations(@Param("userAccountId") Long userAccountId);

    /**
     * An existing open thread for this application, so a moderator opening the
     * same conversation twice lands back in the first one instead of creating a
     * second history nobody will read.
     */
    Optional<Conversation> findFirstByApplication_IdAndTypeAndStatus(
            Long applicationId,
            ConversationType type,
            ConversationStatus status
    );
}
