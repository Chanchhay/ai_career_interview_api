package co.istad.ai_interview_app.config.ai;

import co.istad.ai_interview_app.config.ai.dto.AiModelOverride;
import co.istad.ai_interview_app.config.ai.dto.AiProviderConfigRequest;
import co.istad.ai_interview_app.config.ai.dto.AiProviderConfigResponse;
import co.istad.ai_interview_app.config.ai.dto.AiRuntimeSettings;
import co.istad.ai_interview_app.config.ai.service.AiProviderSettingsService;
import co.istad.ai_interview_app.features.admin.repository.SystemSettingRepository;
import co.istad.ai_interview_app.shared.enums.admin.AiTask;
import co.istad.ai_interview_app.shared.enums.admin.AiThinking;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // A real 32-byte key, base64: without one the console cannot store secrets.
        "app.security.settings-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.ai.google.genai.api-key=env-key",
        "spring.ai.google.genai.chat.model=env-model"
})
class AiProviderSettingsServiceImplTest {

    @Autowired
    private AiProviderSettingsService settingsService;

    @Autowired
    private SystemSettingRepository settingRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void clearStoredSettings() {
        transactionTemplate.executeWithoutResult(status ->
                settingRepository.deleteAll(settingRepository.findAllByCategory("AI_PROVIDER")));
    }

    @Test
    void fallsBackToTheServerConfigurationUntilAnythingIsSaved() {
        AiRuntimeSettings settings = settingsService.current();

        assertThat(settings.apiKey()).isEqualTo("env-key");
        assertThat(settings.apiKeyFromConsole()).isFalse();
        assertThat(settings.model()).isEqualTo("env-model");
        assertThat(settings.modelFor(AiTask.ANSWER_EVALUATION)).isEqualTo("env-model");
    }

    @Test
    void storedKeyOverridesTheEnvironmentAndIsNeverReadBack() {
        settingsService.updateConfig(request("console-model", "super-secret-key-1234", null));

        assertThat(settingsService.current().apiKey()).isEqualTo("super-secret-key-1234");
        assertThat(settingsService.current().apiKeyFromConsole()).isTrue();

        AiProviderConfigResponse response = settingsService.getConfig();
        assertThat(response.apiKeyStored()).isTrue();
        assertThat(response.apiKeyMask()).isEqualTo("••••••1234");
        assertThat(response.model()).isEqualTo("console-model");

        // The one property that matters: nothing on the response carries the key.
        assertThat(response.toString()).doesNotContain("super-secret-key-1234");

        // And it is not sitting in the database in the clear either.
        String stored = settingRepository.findBySettingKey("ai.provider.api_key")
                .orElseThrow()
                .getSettingValue();
        assertThat(stored).startsWith("v1:").doesNotContain("super-secret-key-1234");
    }

    @Test
    void anOmittedKeyLeavesTheStoredOneAloneAndClearingItFallsBackToTheEnvironment() {
        settingsService.updateConfig(request("console-model", "stored-key-abcd", null));

        // A plain save from the console sends no key at all.
        settingsService.updateConfig(request("another-model", null, null));
        assertThat(settingsService.current().apiKey()).isEqualTo("stored-key-abcd");

        AiProviderConfigRequest clearing = new AiProviderConfigRequest(
                "another-model", null, true, List.of(), 0.2, 4096, 120, 2, AiThinking.OFF
        );
        settingsService.updateConfig(clearing);

        assertThat(settingsService.current().apiKey()).isEqualTo("env-key");
        assertThat(settingsService.getConfig().apiKeyStored()).isFalse();
    }

    @Test
    void perTaskModelsOverrideTheDefaultAndClearBackToItWhenRemoved() {
        settingsService.updateConfig(request("default-model", null, List.of(
                new AiModelOverride(AiTask.TRANSCRIPT_SEGMENTATION, "cheap-model"),
                // A blank model is how the console clears one, not an override.
                new AiModelOverride(AiTask.QUESTION_GENERATION, "  ")
        )));

        AiRuntimeSettings settings = settingsService.current();
        assertThat(settings.modelFor(AiTask.TRANSCRIPT_SEGMENTATION)).isEqualTo("cheap-model");
        assertThat(settings.modelFor(AiTask.QUESTION_GENERATION)).isEqualTo("default-model");
        assertThat(settingsService.getConfig().modelOverrides())
                .containsExactly(new AiModelOverride(AiTask.TRANSCRIPT_SEGMENTATION, "cheap-model"));

        settingsService.updateConfig(request("default-model", null, List.of()));

        assertThat(settingsService.current().modelFor(AiTask.TRANSCRIPT_SEGMENTATION))
                .isEqualTo("default-model");
        assertThat(settingsService.getConfig().modelOverrides()).isEmpty();
    }

    private AiProviderConfigRequest request(
            String model,
            String apiKey,
            List<AiModelOverride> overrides
    ) {
        return new AiProviderConfigRequest(
                model, apiKey, false, overrides, 0.2, 4096, 120, 2, AiThinking.OFF
        );
    }
}
