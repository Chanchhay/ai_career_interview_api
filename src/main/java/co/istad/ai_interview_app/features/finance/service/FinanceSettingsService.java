package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.features.finance.dto.FinanceSettingsRequest;
import co.istad.ai_interview_app.features.finance.dto.FinanceSettingsResponse;

public interface FinanceSettingsService {

    FinanceSettingsResponse getSettings();

    FinanceSettingsResponse updateSettings(FinanceSettingsRequest request);
}
