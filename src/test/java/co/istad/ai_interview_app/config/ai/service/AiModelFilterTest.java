package co.istad.ai_interview_app.config.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dropdown is a list of models an administrator may select, so anything in
 * it has to be able to do the platform's work — structured JSON against a
 * schema. A model that would fail on the next interview must not be offered.
 */
class AiModelFilterTest {

    @Test
    void offersChatModels() {
        assertThat(AiProviderSettingsServiceImpl.isUnusable("gemini-3.5-flash")).isFalse();
        assertThat(AiProviderSettingsServiceImpl.isUnusable("gemini-3.5-pro")).isFalse();
        assertThat(AiProviderSettingsServiceImpl.isUnusable("gemini-2.5-flash-lite")).isFalse();
    }

    @Test
    void hidesModelsThatCannotReturnStructuredText() {
        // Wrong output entirely — audio, pictures, video, vectors.
        assertThat(AiProviderSettingsServiceImpl.isUnusable("gemini-embedding-001")).isTrue();
        assertThat(AiProviderSettingsServiceImpl.isUnusable("text-embedding-004")).isTrue();
        assertThat(AiProviderSettingsServiceImpl.isUnusable("imagen-4.0-generate-001")).isTrue();
        assertThat(AiProviderSettingsServiceImpl.isUnusable("veo-3.0-generate-preview")).isTrue();
        assertThat(AiProviderSettingsServiceImpl.isUnusable("gemini-2.5-flash-preview-tts")).isTrue();
        assertThat(AiProviderSettingsServiceImpl.isUnusable("gemini-2.5-flash-image")).isTrue();
        assertThat(AiProviderSettingsServiceImpl.isUnusable("gemini-live-2.5-flash-preview")).isTrue();

        // Right shape of output, but no provider structured-output support.
        assertThat(AiProviderSettingsServiceImpl.isUnusable("gemma-3-27b-it")).isTrue();
    }
}
