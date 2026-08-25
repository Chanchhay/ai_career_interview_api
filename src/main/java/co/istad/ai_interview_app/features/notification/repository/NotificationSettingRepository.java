package co.istad.ai_interview_app.features.notification.repository;

import co.istad.ai_interview_app.features.notification.entity.NotificationSetting;
import co.istad.ai_interview_app.shared.enums.admin.NotificationChannel;
import co.istad.ai_interview_app.shared.enums.admin.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByEventTypeAndChannel(
            NotificationEventType eventType,
            NotificationChannel channel
    );
}
