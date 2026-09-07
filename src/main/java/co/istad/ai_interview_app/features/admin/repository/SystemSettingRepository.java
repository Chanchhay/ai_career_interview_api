package co.istad.ai_interview_app.features.admin.repository;

import co.istad.ai_interview_app.features.admin.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findAllByCategory(String category);
}
