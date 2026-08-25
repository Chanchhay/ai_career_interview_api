package co.istad.ai_interview_app.features.application.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.admin.entity.SystemSetting;
import co.istad.ai_interview_app.features.admin.repository.SystemSettingRepository;
import co.istad.ai_interview_app.features.application.dto.ApplicationSettingsRequest;
import co.istad.ai_interview_app.features.application.dto.ApplicationSettingsResponse;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.shared.enums.admin.SettingValueType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administrator-controlled rules for applying, stored as {@code system_settings}
 * rows — the same pattern as the AI interview and finance settings.
 *
 * <p>Defaults to no cooldown. A platform rule that starts silently switched on
 * would change who can apply the moment this ships, so turning it on is an
 * explicit act by an administrator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationSettingsServiceImpl implements ApplicationSettingsService {

    private static final String CATEGORY = "application";
    private static final String KEY_REAPPLY_COOLDOWN_DAYS = "application.reapply_cooldown_days";
    private static final int DEFAULT_REAPPLY_COOLDOWN_DAYS = 0;

    private final SystemSettingRepository settingRepository;
    private final IdentityUserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public ApplicationSettingsResponse getSettings() {
        return new ApplicationSettingsResponse(reapplyCooldownDays());
    }

    @Override
    @Transactional
    public ApplicationSettingsResponse updateSettings(ApplicationSettingsRequest request) {
        SystemSetting setting = settingRepository.findBySettingKey(KEY_REAPPLY_COOLDOWN_DAYS)
                .orElseGet(() -> {
                    SystemSetting created = new SystemSetting();
                    created.setSettingKey(KEY_REAPPLY_COOLDOWN_DAYS);
                    return created;
                });

        setting.setSettingValue(String.valueOf(request.reapplyCooldownDays()));
        setting.setValueType(SettingValueType.NUMBER);
        setting.setCategory(CATEGORY);
        setting.setDescription(
                "Days a rejected candidate must wait before applying to the same job again. 0 disables it."
        );
        userAccountRepository.findByKeycloakUserId(AuthUtils.extractUserId())
                .ifPresent(setting::setUpdatedByUserAccount);

        settingRepository.save(setting);

        return new ApplicationSettingsResponse(request.reapplyCooldownDays());
    }

    /**
     * A value that will not parse falls back to the default rather than failing.
     * A corrupted settings row must not be able to stop people from applying,
     * and the log line says what happened.
     */
    @Override
    @Transactional(readOnly = true)
    public int reapplyCooldownDays() {
        return settingRepository.findBySettingKey(KEY_REAPPLY_COOLDOWN_DAYS)
                .map(SystemSetting::getSettingValue)
                .map(raw -> {
                    try {
                        return Math.max(0, Integer.parseInt(raw.trim()));
                    } catch (NumberFormatException exception) {
                        log.warn(
                                "Setting {} is not a whole number ({}); treating the cooldown as disabled",
                                KEY_REAPPLY_COOLDOWN_DAYS,
                                raw
                        );
                        return DEFAULT_REAPPLY_COOLDOWN_DAYS;
                    }
                })
                .orElse(DEFAULT_REAPPLY_COOLDOWN_DAYS);
    }
}
