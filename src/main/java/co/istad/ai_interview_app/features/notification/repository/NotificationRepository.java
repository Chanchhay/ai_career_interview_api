package co.istad.ai_interview_app.features.notification.repository;

import co.istad.ai_interview_app.features.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findAllByRecipient_IdAndReadAtIsNullOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable
    );

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);

    Optional<Notification> findByIdAndRecipient_Id(Long id, Long recipientId);

    /**
     * Marks everything unread as read in one statement rather than loading the
     * rows — an inbox left alone for a month should not become a slow request.
     *
     * <p>Bulk updates bypass the persistence context, so callers must not hold
     * stale {@link Notification} instances across this call.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification notification
            set notification.readAt = :readAt
            where notification.recipient.id = :recipientId
              and notification.readAt is null
            """)
    int markAllAsRead(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);
}
