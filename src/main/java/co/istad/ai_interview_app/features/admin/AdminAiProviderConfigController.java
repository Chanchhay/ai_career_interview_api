package co.istad.ai_interview_app.features.admin;

import co.istad.ai_interview_app.config.ai.dto.AiConnectionTestRequest;
import co.istad.ai_interview_app.config.ai.dto.AiConnectionTestResponse;
import co.istad.ai_interview_app.config.ai.dto.AiModelCatalogResponse;
import co.istad.ai_interview_app.config.ai.dto.AiProviderConfigRequest;
import co.istad.ai_interview_app.config.ai.dto.AiProviderConfigResponse;
import co.istad.ai_interview_app.config.ai.service.AiProviderSettingsService;
import co.istad.ai_interview_app.features.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The engine behind every AI feature: which model, on whose API key, with what
 * tuning, per kind of work.
 *
 * <p>Restricted to SUPER_ADMIN in {@code SecurityConfig} rather than sharing the
 * MODERATOR rule the rest of {@code /api/v1/admin/**} uses. The key stored here
 * spends real money, so it is held to the narrowest role the platform has.
 */
@RestController
@RequestMapping("/api/v1/admin/ai-provider-config")
@RequiredArgsConstructor
public class AdminAiProviderConfigController {

    private final AiProviderSettingsService aiProviderSettingsService;

    @GetMapping
    public ApiResponse<AiProviderConfigResponse> getConfig() {
        return ApiResponse.success(aiProviderSettingsService.getConfig());
    }

    @PutMapping
    public ApiResponse<AiProviderConfigResponse> updateConfig(
            @Valid @RequestBody AiProviderConfigRequest request
    ) {
        return ApiResponse.success(aiProviderSettingsService.updateConfig(request));
    }

    /**
     * The models the key in use may actually call, so the console can offer a
     * list rather than a text box that accepts any typo.
     */
    @GetMapping("/models")
    public ApiResponse<AiModelCatalogResponse> availableModels() {
        return ApiResponse.success(aiProviderSettingsService.availableModels());
    }

    /**
     * Sends one tiny prompt so a wrong key or a misspelt model is caught here
     * rather than by the next candidate to start an interview.
     */
    @PostMapping("/test")
    public ApiResponse<AiConnectionTestResponse> testConnection(
            @Valid @RequestBody AiConnectionTestRequest request
    ) {
        return ApiResponse.success(aiProviderSettingsService.testConnection(request));
    }
}
