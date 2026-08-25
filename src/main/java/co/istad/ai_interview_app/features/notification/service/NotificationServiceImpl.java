package co.istad.ai_interview_app.features.notification.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.notification.dto.NewNotification;
import co.istad.ai_interview_app.features.notification.dto.NotificationResponse;
import co.istad.ai_interview_app.features.notification.dto.UnreadCountResponse;
import co.istad.ai_interview_app.features.notification.entity.Notification;
import co.istad.ai_interview_app.features.notification.repository.NotificationRepository;
import co.istad.ai_interview_app.features.notification.repository.NotificationSettingRepository;
import co.istad.ai_interview_app.shared.enums.admin.NotificationChannel;
import co.istad.ai_interview_app.shared.enums.admin.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_TITLE_LENGTH = 200;

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final IdentityUserAccountRepository userAccountRepository;
    private final NotificationStreamService notificationStreamService;

    /* ------------------------------------------------------------- reads --- */

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> findMine(boolean unreadOnly, Pageable pageable) {
        Long recipientId = currentUserAccount().getId();

        Page<Notification> page = unreadOnly
                ? notificationRepository.findAllByRecipient_IdAndReadAtIsNullOrderByCreatedAtDesc(recipientId, pageable)
                : notificationRepository.findAllByRecipient_IdOrderByCreatedAtDesc(recipientId, pageable);

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(
                notificationRepository.countByRecipient_IdAndReadAtIsNull(currentUserAccount().getId())
        );
    }

    /* ------------------------------------------------------------ writes --- */

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndRecipient_Id(notificationId, currentUserAccount().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification was not found"
                ));

        // Idempotent: re-reading something already read must not move its
        // timestamp, or "when was I told" becomes "when did I last look".
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }

        return toResponse(notification);
    }

    @Override
    @Transactional
    public UnreadCountResponse markAllAsRead() {
        notificationRepository.markAllAsRead(currentUserAccount().getId(), Instant.now());
        return new UnreadCountResponse(0);
    }

    @Override
    @Transactional
    public void delete(Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndRecipient_Id(notificationId, currentUserAccount().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification was not found"
                ));

        notificationRepository.delete(notification);
    }

    /* ------------------------------------------------------- creation --- */

    @Override
    @Transactional
    public void create(NewNotification notification) {
        createAll(List.of(notification));
    }

    /**
     * Creating is best-effort by design: it is called from event listeners that
     * run after the business transaction has already committed. A company has
     * been approved whether or not the recruiter's notification row saves, so a
     * failure here is logged, not thrown — rethrowing would only turn a missing
     * notification into an unhandled error in a background listener.
     */
    @Override
    @Transactional
    public void createAll(Collection<NewNotification> notifications) {
        if (notifications.isEmpty()) return;

        List<NewNotification> enabled = notifications.stream()
                .filter(notification -> isEnabled(notification.eventType()))
                .filter(notification -> notification.recipientUserAccountId() != null)
                .toList();

        if (enabled.isEmpty()) return;

        Set<Long> recipientIds = enabled.stream()
                .map(NewNotification::recipientUserAccountId)
                .collect(Collectors.toSet());

        // One lookup for every recipient rather than one per notification: a
        // moderator broadcast addresses the same set repeatedly.
        var accountsById = userAccountRepository.findAllById(recipientIds).stream()
                .collect(Collectors.toMap(UserAccount::getId, account -> account));

        for (NewNotification source : enabled) {
            UserAccount recipient = accountsById.get(source.recipientUserAccountId());

            if (recipient == null) {
                log.warn(
                        "Skipping {} notification: user account {} no longer exists",
                        source.eventType(),
                        source.recipientUserAccountId()
                );
                continue;
            }

            try {
                Notification saved = notificationRepository.save(toEntity(source, recipient));
                notificationStreamService.push(recipient.getId(), toResponse(saved));
            } catch (RuntimeException exception) {
                log.error("Failed to create {} notification", source.eventType(), exception);
            }
        }
    }

    /**
     * Whether an administrator has switched this event off for in-app delivery.
     *
     * <p>Absent configuration means enabled: a new event type should notify by
     * default rather than go silently undelivered until someone remembers to
     * add a row for it.
     *
     * <p>Note that {@code NotificationSetting} also carries subject and body
     * templates. Those are not rendered yet — the wording is composed by the
     * listeners. Honouring the templates needs a template engine and a defined
     * variable set per event, which is its own piece of work.
     */
    private boolean isEnabled(NotificationEventType eventType) {
        return notificationSettingRepository
                .findByEventTypeAndChannel(eventType, NotificationChannel.IN_APP)
                .map(setting -> !Boolean.FALSE.equals(setting.getEnabled()))
                .orElse(true);
    }

    /* ---------------------------------------------------------- helpers --- */

    private UserAccount currentUserAccount() {
        return userAccountRepository.findByKeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account was not found for authenticated user"
                ));
    }

    private Notification toEntity(NewNotification source, UserAccount recipient) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setEventType(source.eventType());
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setTitle(truncateTitle(source.title()));
        notification.setBody(source.body());
        notification.setEntityName(source.entityName());
        notification.setEntityId(source.entityId());
        notification.setActionUrl(source.actionUrl());
        return notification;
    }

    private String truncateTitle(String title) {
        String resolved = Objects.requireNonNullElse(title, "Notification");
        return resolved.length() <= MAX_TITLE_LENGTH
                ? resolved
                : resolved.substring(0, MAX_TITLE_LENGTH);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getEntityName(),
                notification.getEntityId(),
                notification.getActionUrl(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getReadAt() != null
        );
    }
}
