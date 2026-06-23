package co.istad.ai_interview_app.communication.entity;

import co.istad.ai_interview_app.application.entity.JobApplication;
import co.istad.ai_interview_app.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationStatus;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "application_id")
    private JobApplication application;

    @Column(length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConversationType type = ConversationType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConversationStatus status = ConversationStatus.OPEN;

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ConversationParticipant> participants = new ArrayList<>();

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Message> messages = new ArrayList<>();
}