package co.istad.ai_interview_app.config.ai.service;

import co.istad.ai_interview_app.config.ai.dto.AiConnectionTestRequest;
import co.istad.ai_interview_app.config.ai.dto.AiModelCatalogResponse;
import co.istad.ai_interview_app.config.ai.dto.AiConnectionTestResponse;
import co.istad.ai_interview_app.config.ai.dto.AiProviderConfigRequest;
import co.istad.ai_interview_app.config.ai.dto.AiProviderConfigResponse;
import co.istad.ai_interview_app.config.ai.dto.AiRuntimeSettings;

/**
 * The admin-owned settings for talking to the model provider: which model, on
 * whose key, with what tuning.
 */
public interface AiProviderSettingsService {

    AiProviderConfigResponse getConfig();

    AiProviderConfigResponse updateConfig(AiProviderConfigRequest request);

    /** The resolved settings every AI call runs on. */
    AiRuntimeSettings current();

    AiConnectionTestResponse testConnection(AiConnectionTestRequest request);

    /** The models an administrator may pick from, asked of the provider itself. */
    AiModelCatalogResponse availableModels();
}
