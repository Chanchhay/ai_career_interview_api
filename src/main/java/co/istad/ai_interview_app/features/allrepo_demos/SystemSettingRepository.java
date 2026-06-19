package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.admin.domain.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findByCategory(String category);

    boolean existsBySettingKey(String settingKey);
}