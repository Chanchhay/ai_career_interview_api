package co.istad.ai_interview_app.notification.entity;

import co.istad.ai_interview_app.common.audit.BaseEntity;
import co.istad.ai_interview_app.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.admin.NotificationChannel;
import co.istad.ai_interview_app.shared.enums.admin.NotificationEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "notification_settings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_settings_event_channel",
                        columnNames = {"event_type", "channel"}
                )
        }
)
public class NotificationSetting extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationChannel channel;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 200)
    private String subjectTemplate;

    @Column(columnDefinition = "TEXT")
    private String bodyTemplate;

    @ManyToOne
    @JoinColumn(name = "updated_by_user_account_id")
    private UserAccount updatedByUserAccount;
}