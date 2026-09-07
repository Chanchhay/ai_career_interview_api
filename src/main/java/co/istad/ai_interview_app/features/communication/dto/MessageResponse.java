package co.istad.ai_interview_app.features.communication.dto;

import co.istad.ai_interview_app.shared.enums.conversation.MessageStatus;
import co.istad.ai_interview_app.shared.enums.conversation.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderUserAccountId,
        /** True when the caller sent it, so the client need not compare ids. */
        Boolean mine,
        /** Null once deleted — the row survives to keep the thread readable. */
        String content,
        MessageType messageType,
        MessageStatus status,
        Instant sentAt,
        Instant deletedAt
) {
}
