package co.istad.ai_interview_app.features.interview.guest.service;

import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewSettingsRequest;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewSettingsResponse;

public interface GuestInterviewSettingsService {

    GuestInterviewSettingsResponse getSettings();

    GuestInterviewSettingsResponse updateSettings(GuestInterviewSettingsRequest request);
}
