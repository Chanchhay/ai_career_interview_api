package co.istad.ai_interview_app.features.communication.domain;

import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.domain.UserAccount;
import co.istad.ai_interview_app.shared.enums.conversation.MessageStatus;
import co.istad.ai_interview_app.shared.enums.conversation.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_user_account_id", nullable = false)
    private UserAccount senderUserAccount;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageType messageType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageStatus status = MessageStatus.SENT;

    @Column(nullable = false)
    private Instant sentAt = Instant.now();

    private Instant readAt;

    private Instant deletedAt;
}