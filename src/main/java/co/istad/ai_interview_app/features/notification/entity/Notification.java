package co.istad.ai_interview_app.features.notification.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.admin.NotificationChannel;
import co.istad.ai_interview_app.shared.enums.admin.NotificationEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One delivered notification, addressed to one account.
 *
 * <p>Distinct from {@link NotificationSetting}, which is the administrator's
 * template and on/off switch per event type — this is the message a person
 * actually receives.
 *
 * <p>The text is stored rather than rendered on read. A notification is a
 * record of what someone was told at a point in time; regenerating it later
 * from a template that has since been edited, or from a company that has since
 * been renamed, would quietly rewrite history.
 */
@Getter
@Setter
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notifications_recipient_created",
                        columnList = "recipient_user_account_id, created_at"
                ),
                @Index(
                        name = "idx_notifications_recipient_read",
                        columnList = "recipient_user_account_id, read_at"
                )
        }
)
public class Notification extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_account_id", nullable = false)
    private UserAccount recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    /** What the notification is about, for de-duplication and deep links. */
    @Column(name = "entity_name", length = 120)
    private String entityName;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    /**
     * Where the recipient should land when they click through. Stored as an
     * app-relative path — the same notification is read from two different
     * front ends, so an absolute URL would be wrong in one of them.
     */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /** Null while unread. Doubles as the read timestamp, so no second column. */
    @Column(name = "read_at")
    private Instant readAt;
}
