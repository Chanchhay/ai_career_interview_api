package co.istad.ai_interview_app.features.communication.service;

import co.istad.ai_interview_app.features.communication.dto.ConversationResponse;
import co.istad.ai_interview_app.features.communication.dto.CreateConversationRequest;
import co.istad.ai_interview_app.features.communication.dto.MessageResponse;
import co.istad.ai_interview_app.features.communication.dto.OpenSupportRequest;
import co.istad.ai_interview_app.features.communication.dto.SendMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConversationService {

    /* Available to every participant. */

    Page<ConversationResponse> findMyConversations(Pageable pageable);

    ConversationResponse getConversation(Long conversationId);

    Page<MessageResponse> findMessages(Long conversationId, Pageable pageable);

    MessageResponse sendMessage(Long conversationId, SendMessageRequest request);

    ConversationResponse markAsRead(Long conversationId);

    void deleteMessage(Long conversationId, Long messageId);

    /**
     * Opens (or continues) the caller's support thread with the moderator team.
     * The one creation path available to a non-moderator.
     */
    ConversationResponse openSupportConversation(OpenSupportRequest request);

    /* Moderator only. */

    ConversationResponse createConversation(CreateConversationRequest request);

    ConversationResponse closeConversation(Long conversationId);
}
