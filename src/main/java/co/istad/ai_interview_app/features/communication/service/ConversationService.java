package co.istad.ai_interview_app.features.communication.service;

import co.istad.ai_interview_app.features.communication.dto.ConversationResponse;
import co.istad.ai_interview_app.features.communication.dto.CreateConversationRequest;
import co.istad.ai_interview_app.features.communication.dto.MessageResponse;
import co.istad.ai_interview_app.features.communication.dto.OpenSupportRequest;
import co.istad.ai_interview_app.features.communication.dto.SendMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface ConversationService {

    /* Available to every participant. */

    Page<ConversationResponse> findMyConversations(Pageable pageable);

    ConversationResponse getConversation(UUID conversationId);

    Page<MessageResponse> findMessages(UUID conversationId, Pageable pageable);

    MessageResponse sendMessage(UUID conversationId, SendMessageRequest request);

    ConversationResponse markAsRead(UUID conversationId);

    void deleteMessage(UUID conversationId, UUID messageId);

    /**
     * Opens (or continues) the caller's support thread with the moderator team.
     * The one creation path available to a non-moderator.
     */
    ConversationResponse openSupportConversation(OpenSupportRequest request);

    /* Moderator only. */

    ConversationResponse createConversation(CreateConversationRequest request);

    ConversationResponse closeConversation(UUID conversationId);
}
