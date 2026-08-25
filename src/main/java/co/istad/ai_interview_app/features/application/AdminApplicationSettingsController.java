package co.istad.ai_interview_app.features.application;

import co.istad.ai_interview_app.features.application.dto.ApplicationSettingsRequest;
import co.istad.ai_interview_app.features.application.dto.ApplicationSettingsResponse;
import co.istad.ai_interview_app.features.application.service.ApplicationSettingsService;
import co.istad.ai_interview_app.features.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The rules governing how candidates may apply.
 *
 * <p>Lives under {@code /api/v1/admin/**}, so MODERATOR and above can change it
 * — the same rule as the AI interview configuration and the shared taxonomy.
 */
@RestController
@RequestMapping("/api/v1/admin/application-settings")
@RequiredArgsConstructor
public class AdminApplicationSettingsController {

    private final ApplicationSettingsService applicationSettingsService;

    @GetMapping
    public ApiResponse<ApplicationSettingsResponse> getSettings() {
        return ApiResponse.success(applicationSettingsService.getSettings());
    }

    @PutMapping
    public ApiResponse<ApplicationSettingsResponse> updateSettings(
            @Valid @RequestBody ApplicationSettingsRequest request
    ) {
        return ApiResponse.success(applicationSettingsService.updateSettings(request));
    }
}
