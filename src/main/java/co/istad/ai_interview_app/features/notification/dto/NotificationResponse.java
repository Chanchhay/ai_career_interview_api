package co.istad.ai_interview_app.features.notification.dto;

import co.istad.ai_interview_app.shared.enums.admin.NotificationEventType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationEventType eventType,
        String title,
        String body,
        String entityName,
        String entityId,
        /** App-relative path the client should navigate to, or null. */
        String actionUrl,
        Instant createdAt,
        Instant readAt,
        Boolean read
) {
}
