package co.istad.ai_interview_app.features.interview.guest;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewSettingsRequest;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewSettingsResponse;
import co.istad.ai_interview_app.features.interview.guest.service.GuestInterviewSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * How many interviews a guest may take, and where their questions come from.
 *
 * <p>Sits with the other {@code /api/v1/admin/**} settings, so moderators reach
 * it and SUPER_ADMIN reaches it through the role hierarchy.
 */
@RestController
@RequestMapping("/api/v1/admin/guest-interview-settings")
@RequiredArgsConstructor
public class AdminGuestInterviewSettingsController {

    private final GuestInterviewSettingsService settingsService;

    @GetMapping
    public ApiResponse<GuestInterviewSettingsResponse> getSettings() {
        return ApiResponse.success(settingsService.getSettings());
    }

    @PutMapping
    public ApiResponse<GuestInterviewSettingsResponse> updateSettings(
            @Valid @RequestBody GuestInterviewSettingsRequest request
    ) {
        return ApiResponse.success(settingsService.updateSettings(request));
    }
}
