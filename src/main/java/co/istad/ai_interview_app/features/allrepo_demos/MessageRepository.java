package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.communication.entity.Message;
import co.istad.ai_interview_app.shared.enums.conversation.MessageStatus;
import co.istad.ai_interview_app.shared.enums.conversation.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversation_IdOrderBySentAtAsc(Long conversationId);

    List<Message> findBySenderUserAccount_Id(Long senderUserAccountId);

    List<Message> findByConversation_IdAndStatusOrderBySentAtAsc(
            Long conversationId,
            MessageStatus status
    );

    List<Message> findByConversation_IdAndMessageTypeOrderBySentAtAsc(
            Long conversationId,
            MessageType messageType
    );
}