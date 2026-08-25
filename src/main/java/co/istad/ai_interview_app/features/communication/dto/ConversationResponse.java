package co.istad.ai_interview_app.features.communication.dto;

import co.istad.ai_interview_app.shared.enums.conversation.ConversationStatus;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationType;

import java.time.Instant;
import java.util.List;

public record ConversationResponse(
        Long id,
        String title,
        ConversationType type,
        ConversationStatus status,
        /** Set for APPLICATION threads, so the client can deep-link the case. */
        Long applicationId,
        List<ConversationParticipantResponse> participants,
        MessageResponse lastMessage,
        long unreadCount,
        Instant createdAt
) {
}
