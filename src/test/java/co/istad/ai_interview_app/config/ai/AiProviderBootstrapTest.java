package co.istad.ai_interview_app.config.ai;

import co.istad.ai_interview_app.config.ai.dto.AiModelCatalogResponse;
import co.istad.ai_interview_app.config.ai.service.AiProviderSettingsService;
import co.istad.ai_interview_app.shared.enums.admin.AiTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The console can only be used to set the API key if the server starts without
 * one. This pins that: no {@code GEMINI_API_KEY}, context still loads.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.ai.google.genai.api-key=")
class AiProviderBootstrapTest {

    @Autowired
    private AiProviderSettingsService settingsService;

    @Autowired
    private AiChatClientFactory chatClientFactory;

    @Test
    void startsWithNoApiKeyAndReportsTheGapInsteadOfFailingAtBoot() {
        assertThat(settingsService.getConfig().apiKeyStored()).isFalse();
        assertThat(settingsService.getConfig().environmentKeyAvailable()).isFalse();

        // The gap surfaces where it can be explained — at the call site — rather
        // than as a failed startup nobody can log in to fix.
        assertThatThrownBy(() -> chatClientFactory.forTask(AiTask.QUESTION_GENERATION))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No AI API key is configured");
    }

    /**
     * With no key there is nobody to ask what models exist, and an empty
     * dropdown would be a dead end on the very screen where the key is set.
     */
    @Test
    void offersAFallbackModelListWhenTheProviderCannotBeAsked() {
        AiModelCatalogResponse catalog = settingsService.availableModels();

        assertThat(catalog.live()).isFalse();
        assertThat(catalog.message()).contains("No API key is configured");
        assertThat(catalog.models()).isNotEmpty();

        // Whatever is configured is always selectable, so opening the dropdown
        // cannot silently change the model in use.
        assertThat(catalog.models())
                .extracting(option -> option.id())
                .contains(settingsService.current().model());
    }
}
