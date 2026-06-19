package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.admin.domain.NotificationSetting;
import co.istad.ai_interview_app.shared.enums.admin.NotificationChannel;
import co.istad.ai_interview_app.shared.enums.admin.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    List<NotificationSetting> findByEventType(NotificationEventType eventType);

    List<NotificationSetting> findByChannel(NotificationChannel channel);

    Optional<NotificationSetting> findByEventTypeAndChannel(
            NotificationEventType eventType,
            NotificationChannel channel
    );

    List<NotificationSetting> findByEnabled(Boolean enabled);
}