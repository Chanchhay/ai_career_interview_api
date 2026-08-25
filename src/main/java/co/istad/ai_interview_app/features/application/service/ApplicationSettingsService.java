package co.istad.ai_interview_app.features.application.service;

import co.istad.ai_interview_app.features.application.dto.ApplicationSettingsRequest;
import co.istad.ai_interview_app.features.application.dto.ApplicationSettingsResponse;

public interface ApplicationSettingsService {

    ApplicationSettingsResponse getSettings();

    ApplicationSettingsResponse updateSettings(ApplicationSettingsRequest request);

    /** The configured cooldown, read on the application path. */
    int reapplyCooldownDays();
}
