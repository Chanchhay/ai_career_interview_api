package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.admin.entity.SystemSetting;
import co.istad.ai_interview_app.features.admin.repository.SystemSettingRepository;
import co.istad.ai_interview_app.features.finance.dto.FinanceSettingsRequest;
import co.istad.ai_interview_app.features.finance.dto.FinanceSettingsResponse;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.shared.enums.admin.SettingValueType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

/**
 * Platform-wide billing settings, stored as {@code system_settings} rows —
 * the same pattern the AI interview configuration uses.
 *
 * <p>These are defaults for future commissions only. A commission freezes the
 * rate it was created with, so editing the rate here never restates a bill
 * somebody has already been sent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceSettingsServiceImpl implements FinanceSettingsService {

    private static final String CATEGORY = "finance";
    private static final String KEY_COMMISSION_RATE = "finance.commission_rate";
    private static final String KEY_PAYMENT_TERMS_DAYS = "finance.payment_terms_days";
    private static final String KEY_CURRENCY = "finance.currency";

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("10.00");
    private static final int DEFAULT_PAYMENT_TERMS_DAYS = 30;
    private static final String DEFAULT_CURRENCY = "USD";

    private final SystemSettingRepository settingRepository;
    private final IdentityUserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public FinanceSettingsResponse getSettings() {
        return new FinanceSettingsResponse(
                readDecimal(KEY_COMMISSION_RATE, DEFAULT_COMMISSION_RATE),
                readInt(KEY_PAYMENT_TERMS_DAYS, DEFAULT_PAYMENT_TERMS_DAYS),
                readString(KEY_CURRENCY, DEFAULT_CURRENCY)
        );
    }

    @Override
    @Transactional
    public FinanceSettingsResponse updateSettings(FinanceSettingsRequest request) {
        writeSetting(
                KEY_COMMISSION_RATE,
                request.commissionRate().toPlainString(),
                SettingValueType.NUMBER,
                "Percentage of the offered salary charged to a company when a hire is confirmed."
        );
        writeSetting(
                KEY_PAYMENT_TERMS_DAYS,
                String.valueOf(request.paymentTermsDays()),
                SettingValueType.NUMBER,
                "Days from issue to due date when an invoice does not name one."
        );
        writeSetting(
                KEY_CURRENCY,
                hasText(request.currency()) ? request.currency().toUpperCase() : DEFAULT_CURRENCY,
                SettingValueType.STRING,
                "Currency commissions and invoices are denominated in."
        );

        return getSettings();
    }

    /* ----------------------------------------------------------- reading --- */

    private String readString(String key, String fallback) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(value -> hasText(value))
                .orElse(fallback);
    }

    /**
     * A value that cannot be parsed falls back to the default rather than
     * failing the request. A corrupted settings row must not be able to stop
     * hires from being confirmed — and the log line says what happened.
     */
    private BigDecimal readDecimal(String key, BigDecimal fallback) {
        String raw = readString(key, null);
        if (raw == null) return fallback;

        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException exception) {
            log.warn("Setting {} is not a number ({}); using {}", key, raw, fallback);
            return fallback;
        }
    }

    private int readInt(String key, int fallback) {
        String raw = readString(key, null);
        if (raw == null) return fallback;

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            log.warn("Setting {} is not a whole number ({}); using {}", key, raw, fallback);
            return fallback;
        }
    }

    private void writeSetting(String key, String value, SettingValueType type, String description) {
        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting created = new SystemSetting();
                    created.setSettingKey(key);
                    created.setCategory(CATEGORY);
                    return created;
                });

        setting.setSettingValue(value);
        setting.setValueType(type);
        setting.setDescription(description);
        setting.setCategory(CATEGORY);
        userAccountRepository.findByKeycloakUserId(AuthUtils.extractUserId())
                .ifPresent(setting::setUpdatedByUserAccount);

        settingRepository.save(setting);
    }
}
