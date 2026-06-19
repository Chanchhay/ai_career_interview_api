package co.istad.ai_interview_app.features.communication.domain;

import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.domain.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "conversation_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversation_participants_conversation_user",
                        columnNames = {"conversation_id", "user_account_id"}
                )
        }
)
public class ConversationParticipant extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    private Instant lastReadAt;

    private Instant leftAt;

    @Column(nullable = false)
    private Boolean muted = false;
}