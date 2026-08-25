package co.istad.ai_interview_app.features.notification.dto;

import co.istad.ai_interview_app.shared.enums.admin.NotificationEventType;

/**
 * What a listener hands to the notification service. Deliberately not a request
 * DTO — nothing outside the application may create a notification, so this
 * shape is never bound from a request body.
 */
public record NewNotification(
        Long recipientUserAccountId,
        NotificationEventType eventType,
        String title,
        String body,
        String entityName,
        String entityId,
        String actionUrl
) {
}
