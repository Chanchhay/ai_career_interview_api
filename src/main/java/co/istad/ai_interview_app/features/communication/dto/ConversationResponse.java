package co.istad.ai_interview_app.features.communication.dto;

import co.istad.ai_interview_app.shared.enums.conversation.ConversationStatus;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String title,
        ConversationType type,
        ConversationStatus status,
        /** Set for APPLICATION threads, so the client can deep-link the case. */
        UUID applicationId,
        String jobTitle,
        List<ConversationParticipantResponse> participants,
        MessageResponse lastMessage,
        long unreadCount,
        Instant createdAt
) {
}
