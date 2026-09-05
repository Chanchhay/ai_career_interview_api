package co.istad.ai_interview_app.features.interview.guest.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.admin.entity.SystemSetting;
import co.istad.ai_interview_app.features.admin.repository.SystemSettingRepository;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewSettingsRequest;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewSettingsResponse;
import co.istad.ai_interview_app.shared.enums.admin.SettingValueType;
import co.istad.ai_interview_app.shared.enums.interview.GuestQuestionSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest interview rules, stored as {@code system_settings} rows — the same
 * pattern as the AI interview, finance and application settings.
 *
 * <p>Defaults to <strong>off</strong>. Letting strangers spend the platform's
 * AI budget is not something a release should switch on by itself; an
 * administrator turns it on and chooses the limits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestInterviewSettingsServiceImpl implements GuestInterviewSettingsService {

    private static final String CATEGORY = "guest_interview";
    private static final String KEY_ENABLED = "guest_interview.enabled";
    private static final String KEY_MAX_PER_GUEST = "guest_interview.max_attempts_per_guest";
    private static final String KEY_MAX_PER_IP_PER_DAY = "guest_interview.max_attempts_per_ip_per_day";
    private static final String KEY_QUESTION_SOURCE = "guest_interview.question_source";

    private static final boolean DEFAULT_ENABLED = false;
    private static final int DEFAULT_MAX_PER_GUEST = 1;
    private static final int DEFAULT_MAX_PER_IP_PER_DAY = 20;
    private static final GuestQuestionSource DEFAULT_QUESTION_SOURCE = GuestQuestionSource.FOLLOW_JOB;

    private final SystemSettingRepository settingRepository;
    private final IdentityUserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public GuestInterviewSettingsResponse getSettings() {
        return new GuestInterviewSettingsResponse(
                readBoolean(KEY_ENABLED, DEFAULT_ENABLED),
                readInt(KEY_MAX_PER_GUEST, DEFAULT_MAX_PER_GUEST),
                readInt(KEY_MAX_PER_IP_PER_DAY, DEFAULT_MAX_PER_IP_PER_DAY),
                readQuestionSource()
        );
    }

    @Override
    @Transactional
    public GuestInterviewSettingsResponse updateSettings(GuestInterviewSettingsRequest request) {
        save(KEY_ENABLED, String.valueOf(request.enabled()), SettingValueType.BOOLEAN,
                "Whether people who are not signed in may take an AI interview.");
        save(KEY_MAX_PER_GUEST, String.valueOf(request.maxAttemptsPerGuest()), SettingValueType.NUMBER,
                "How many AI interviews one guest may take. 0 stops guests starting one.");
        save(KEY_MAX_PER_IP_PER_DAY, String.valueOf(request.maxAttemptsPerIpPerDay()), SettingValueType.NUMBER,
                "How many guest AI interviews may start from one network per day.");
        save(KEY_QUESTION_SOURCE, request.questionSource().name(), SettingValueType.STRING,
                "Where guest interview questions come from: the job's own setting, written only, or always generated.");

        return new GuestInterviewSettingsResponse(
                request.enabled(),
                request.maxAttemptsPerGuest(),
                request.maxAttemptsPerIpPerDay(),
                request.questionSource()
        );
    }

    private void save(String key, String value, SettingValueType type, String description) {
        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting created = new SystemSetting();
                    created.setSettingKey(key);
                    return created;
                });

        setting.setSettingValue(value);
        setting.setValueType(type);
        setting.setCategory(CATEGORY);
        setting.setDescription(description);
        userAccountRepository.findByKeycloakUserId(AuthUtils.extractUserId())
                .ifPresent(setting::setUpdatedByUserAccount);

        settingRepository.save(setting);
    }

    /*
     * Every read falls back to the default rather than failing. A settings row
     * edited by hand into nonsense must not take the public site down, and the
     * fallbacks are all the cautious direction: off, and the smaller number.
     */

    private boolean readBoolean(String key, boolean fallback) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .map(raw -> Boolean.parseBoolean(raw.trim()))
                .orElse(fallback);
    }

    private int readInt(String key, int fallback) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .map(raw -> {
                    try {
                        return Math.max(0, Integer.parseInt(raw.trim()));
                    } catch (NumberFormatException exception) {
                        log.warn("Setting {} is not a whole number ({}); using {}", key, raw, fallback);
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private GuestQuestionSource readQuestionSource() {
        return settingRepository.findBySettingKey(KEY_QUESTION_SOURCE)
                .map(SystemSetting::getSettingValue)
                .map(raw -> {
                    try {
                        return GuestQuestionSource.valueOf(raw.trim());
                    } catch (IllegalArgumentException exception) {
                        log.warn("Setting {} is not a known source ({}); using {}",
                                KEY_QUESTION_SOURCE, raw, DEFAULT_QUESTION_SOURCE);
                        return DEFAULT_QUESTION_SOURCE;
                    }
                })
                .orElse(DEFAULT_QUESTION_SOURCE);
    }
}
