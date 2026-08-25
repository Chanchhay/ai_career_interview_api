package co.istad.ai_interview_app.features.notification.service;

import co.istad.ai_interview_app.features.notification.dto.NewNotification;
import co.istad.ai_interview_app.features.notification.dto.NotificationResponse;
import co.istad.ai_interview_app.features.notification.dto.UnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface NotificationService {

    Page<NotificationResponse> findMine(boolean unreadOnly, Pageable pageable);

    UnreadCountResponse unreadCount();

    NotificationResponse markAsRead(Long notificationId);

    UnreadCountResponse markAllAsRead();

    void delete(Long notificationId);

    /** Creates and delivers one notification. Called by listeners, never by a controller. */
    void create(NewNotification notification);

    void createAll(Collection<NewNotification> notifications);
}
