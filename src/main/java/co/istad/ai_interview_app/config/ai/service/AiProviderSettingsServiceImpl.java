package co.istad.ai_interview_app.config.ai.service;

import co.istad.ai_interview_app.config.ai.AiChatClientFactory;
import co.istad.ai_interview_app.config.ai.dto.AiConnectionTestRequest;
import co.istad.ai_interview_app.config.ai.dto.AiConnectionTestResponse;
import co.istad.ai_interview_app.config.ai.dto.AiModelCatalogResponse;
import co.istad.ai_interview_app.config.ai.dto.AiModelOption;
import co.istad.ai_interview_app.config.ai.dto.AiModelOverride;
import co.istad.ai_interview_app.config.ai.dto.AiProviderConfigRequest;
import co.istad.ai_interview_app.config.ai.dto.AiProviderConfigResponse;
import co.istad.ai_interview_app.config.ai.dto.AiRuntimeSettings;
import co.istad.ai_interview_app.features.admin.entity.SystemSetting;
import co.istad.ai_interview_app.features.admin.repository.SystemSettingRepository;
import co.istad.ai_interview_app.shared.crypto.SecretCipher;
import co.istad.ai_interview_app.shared.enums.admin.AiTask;
import co.istad.ai_interview_app.shared.enums.admin.AiThinking;
import co.istad.ai_interview_app.shared.enums.admin.SettingValueType;
import com.google.genai.Client;
import com.google.genai.types.ListModelsConfig;
import com.google.genai.types.Model;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Keeps the provider settings in {@code system_settings}, one key per setting,
 * so the model, the credentials and the tuning can change without a deployment.
 *
 * <p>Every value falls back to the deployment's own configuration when no row
 * exists, so an untouched install behaves exactly as it did before this screen
 * existed — including running on the {@code GEMINI_API_KEY} the server was
 * started with.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderSettingsServiceImpl implements AiProviderSettingsService {

    private static final String CATEGORY = "AI_PROVIDER";

    private static final String KEY_API_KEY = "ai.provider.api_key";
    private static final String KEY_MODEL = "ai.provider.model";
    private static final String KEY_TEMPERATURE = "ai.provider.temperature";
    private static final String KEY_MAX_OUTPUT_TOKENS = "ai.provider.max_output_tokens";
    private static final String KEY_TIMEOUT_SECONDS = "ai.provider.timeout_seconds";
    private static final String KEY_MAX_RETRIES = "ai.provider.max_retries";
    private static final String KEY_THINKING = "ai.provider.thinking";
    private static final String KEY_MODEL_PREFIX = "ai.provider.model.";

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;
    private static final int DEFAULT_MAX_RETRIES = 2;

    /**
     * Off by default: every call this platform makes is structured extraction,
     * where deliberation costs seconds and buys little. An administrator can
     * trade the speed back for depth on the AI engine screen.
     */
    private static final AiThinking DEFAULT_THINKING = AiThinking.OFF;

    private static final int MODEL_PAGE_SIZE = 100;
    private static final int MAX_MODELS = 200;

    /**
     * Shown when the provider cannot be asked. Deliberately short: it exists so
     * the dropdown is never empty, not to be an accurate catalogue, and it says
     * as much through {@code live: false}.
     */
    private static final List<AiModelOption> FALLBACK_MODELS = List.of(
            new AiModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", "Fast and cheap; the platform default."),
            new AiModelOption("gemini-3.5-pro", "Gemini 3.5 Pro", "Stronger reasoning, slower and dearer."),
            new AiModelOption("gemini-2.5-flash", "Gemini 2.5 Flash", "Previous generation, fast."),
            new AiModelOption("gemini-2.5-pro", "Gemini 2.5 Pro", "Previous generation, stronger.")
    );

    private final SystemSettingRepository settingRepository;
    private final SecretCipher secretCipher;
    private final AiChatClientFactory chatClientFactory;

    /** What the server was started with; the floor every setting falls back to. */
    @Value("${spring.ai.google.genai.api-key:}")
    private String environmentApiKey;

    @Value("${spring.ai.google.genai.chat.model:gemini-3.5-flash}")
    private String environmentModel;

    @Value("${spring.ai.google.genai.chat.temperature:0.2}")
    private Double environmentTemperature;

    @Value("${spring.ai.google.genai.chat.max-output-tokens:4096}")
    private Integer environmentMaxOutputTokens;

    @Override
    @Transactional(readOnly = true)
    public AiProviderConfigResponse getConfig() {
        Map<String, SystemSetting> stored = storedSettings();
        AiRuntimeSettings current = resolve(stored);

        String storedKey = readSecret(stored, KEY_API_KEY);

        Instant updatedAt = null;
        String updatedBy = null;
        for (SystemSetting setting : stored.values()) {
            if (setting.getUpdatedAt() != null
                    && (updatedAt == null || setting.getUpdatedAt().isAfter(updatedAt))) {
                updatedAt = setting.getUpdatedAt();
                updatedBy = setting.getUpdatedBy();
            }
        }

        return new AiProviderConfigResponse(
                current.model(),
                SecretCipher.mask(hasText(storedKey) ? storedKey : environmentApiKey),
                hasText(storedKey),
                hasText(environmentApiKey),
                secretCipher.isConfigured(),
                overridesFor(stored),
                Arrays.asList(AiTask.values()),
                current.temperature(),
                current.maxOutputTokens(),
                (int) current.timeout().toSeconds(),
                current.maxRetries(),
                current.thinking(),
                Arrays.asList(AiThinking.values()),
                updatedAt,
                updatedBy
        );
    }

    @Override
    @Transactional
    public AiProviderConfigResponse updateConfig(AiProviderConfigRequest request) {
        writeSetting(KEY_MODEL, request.model().trim(), SettingValueType.STRING,
                "Default model every AI feature runs on.");
        writeSetting(KEY_TEMPERATURE, String.valueOf(request.temperature()), SettingValueType.NUMBER,
                "How much the model is allowed to vary its wording.");
        writeSetting(KEY_MAX_OUTPUT_TOKENS, String.valueOf(request.maxOutputTokens()), SettingValueType.NUMBER,
                "Ceiling on the length of a single model response.");
        writeSetting(KEY_TIMEOUT_SECONDS, String.valueOf(request.timeoutSeconds()), SettingValueType.NUMBER,
                "How long to wait for the provider before giving up.");
        writeSetting(KEY_MAX_RETRIES, String.valueOf(request.maxRetries()), SettingValueType.NUMBER,
                "How many times to retry a failed provider call.");

        writeSetting(KEY_THINKING, request.thinking().name(), SettingValueType.STRING,
                "How much the model may deliberate before answering.");

        writeModelOverrides(request.modelOverrides());
        writeApiKey(request);

        // The factory caches a built client per distinct set of settings, so a
        // save has to invalidate it or the next interview would run on what the
        // administrator just changed away from.
        chatClientFactory.settingsChanged();

        return getConfig();
    }

    @Override
    @Transactional(readOnly = true)
    public AiRuntimeSettings current() {
        return resolve(storedSettings());
    }

    @Override
    public AiConnectionTestResponse testConnection(AiConnectionTestRequest request) {
        AiRuntimeSettings settings = current();

        String model = hasText(request.model()) ? request.model().trim() : settings.model();
        boolean keyTyped = hasText(request.apiKey());
        String apiKey = keyTyped ? request.apiKey().trim() : settings.apiKey();
        String keySource = keyTyped
                ? "console"
                : settings.apiKeyFromConsole() ? "console" : "environment";

        if (!hasText(apiKey)) {
            return new AiConnectionTestResponse(
                    false, model, keySource, null,
                    "No API key is configured. Enter one above, or set GEMINI_API_KEY on the server."
            );
        }

        long startedAt = System.nanoTime();
        try {
            String reply = chatClientFactory
                    .clientFor(apiKey, model, settings)
                    .prompt()
                    .user("Reply with the single word: ready")
                    .call()
                    .content();

            long millis = (System.nanoTime() - startedAt) / 1_000_000;

            return new AiConnectionTestResponse(
                    true, model, keySource, millis,
                    "The provider answered: " + normalizeBlankToNull(reply)
            );
        } catch (Exception ex) {
            long millis = (System.nanoTime() - startedAt) / 1_000_000;
            log.warn("AI connection test failed for model={}", model, ex);

            return new AiConnectionTestResponse(
                    false, model, keySource, millis, rootMessage(ex)
            );
        }
    }

    @Override
    public AiModelCatalogResponse availableModels() {
        AiRuntimeSettings settings = current();

        // Whatever is configured is always offered, even when the provider will
        // not answer: a dropdown that cannot show the model currently in use
        // would silently change it the moment an administrator saves.
        Set<String> configured = new LinkedHashSet<>();
        configured.add(settings.model());
        settings.modelByTask().values().forEach(configured::add);

        if (!hasText(settings.apiKey())) {
            return new AiModelCatalogResponse(
                    false,
                    "No API key is configured, so this is a built-in list rather than "
                            + "the models your key can actually call.",
                    withConfigured(FALLBACK_MODELS, configured)
            );
        }

        try {
            List<AiModelOption> models = fetchModels(settings);

            if (models.isEmpty()) {
                return new AiModelCatalogResponse(
                        false,
                        "The provider returned no usable models, so this is a built-in list.",
                        withConfigured(FALLBACK_MODELS, configured)
                );
            }

            return new AiModelCatalogResponse(
                    true,
                    "Listed by the provider for the key in use.",
                    withConfigured(models, configured)
            );
        } catch (Exception ex) {
            log.warn("Could not list models from the provider", ex);

            return new AiModelCatalogResponse(
                    false,
                    "Could not reach the provider (" + rootMessage(ex) + "), so this is a built-in list.",
                    withConfigured(FALLBACK_MODELS, configured)
            );
        }
    }

    private List<AiModelOption> fetchModels(AiRuntimeSettings settings) {
        Client client = Client.builder()
                .apiKey(settings.apiKey())
                .httpOptions(com.google.genai.types.HttpOptions.builder()
                        .timeout(Math.toIntExact(settings.timeout().toMillis()))
                        .build())
                .build();

        List<AiModelOption> models = new ArrayList<>();

        // queryBase lists the provider's own models rather than tuned copies.
        for (Model model : client.models.list(ListModelsConfig.builder()
                .queryBase(true)
                .pageSize(MODEL_PAGE_SIZE)
                .build())) {

            if (models.size() >= MAX_MODELS) {
                break;
            }

            // Only models that say they can generate content. Anything that does
            // not claim the ability is left out rather than offered on the hope
            // that it works — a model in this list is a model an administrator
            // can select, and selecting a broken one breaks every AI feature.
            boolean generates = model.supportedActions()
                    .map(actions -> actions.contains("generateContent"))
                    .orElse(false);
            if (!generates) {
                continue;
            }

            String id = model.name().map(AiProviderSettingsServiceImpl::stripPrefix).orElse(null);
            if (!hasText(id) || isUnusable(id)) {
                continue;
            }

            models.add(new AiModelOption(
                    id,
                    model.displayName().filter(name -> hasText(name)).orElse(id),
                    model.description().orElse(null)
            ));
        }

        // Newest generation first: reverse order puts gemini-3.x above 2.x, which
        // is the order an administrator is choosing in anyway.
        models.sort(Comparator.comparing(AiModelOption::id).reversed());
        return models;
    }

    /**
     * Models that answer {@code generateContent} but still cannot do this
     * platform's work.
     *
     * <p>Every call here asks for structured JSON against a schema. Speech,
     * image and video models will not produce it; embedding and answering
     * models are a different shape of task entirely; and the open Gemma weights
     * do not support the provider's structured-output mode. Listing any of them
     * would offer an administrator a choice that fails on the next interview.
     */
    static boolean isUnusable(String id) {
        String model = id.toLowerCase();

        return model.contains("embedding")
                || model.contains("embed")
                || model.contains("aqa")
                || model.contains("imagen")
                || model.contains("veo")
                || model.contains("tts")
                || model.contains("image")
                || model.contains("audio")
                || model.contains("live")
                || model.contains("gemma")
                || model.contains("learnlm");
    }

    /** `models/gemini-3.5-flash` → `gemini-3.5-flash`. */
    private static String stripPrefix(String name) {
        return name.startsWith("models/") ? name.substring("models/".length()) : name;
    }

    private List<AiModelOption> withConfigured(List<AiModelOption> models, Set<String> configured) {
        List<AiModelOption> combined = new ArrayList<>(models);

        for (String model : configured) {
            if (!hasText(model)) {
                continue;
            }
            boolean known = combined.stream().anyMatch(option -> option.id().equals(model));
            if (!known) {
                combined.add(new AiModelOption(model, model + " (in use)", "Currently configured."));
            }
        }

        return combined;
    }

    /* ------------------------------------------------------------ helpers --- */

    private AiRuntimeSettings resolve(Map<String, SystemSetting> stored) {
        String storedKey = readSecret(stored, KEY_API_KEY);
        boolean fromConsole = hasText(storedKey);

        Map<AiTask, String> modelByTask = new EnumMap<>(AiTask.class);
        for (AiTask task : AiTask.values()) {
            String model = readString(stored, KEY_MODEL_PREFIX + task.name());
            if (hasText(model)) {
                modelByTask.put(task, model);
            }
        }

        return new AiRuntimeSettings(
                fromConsole ? storedKey : environmentApiKey,
                fromConsole,
                orDefault(readString(stored, KEY_MODEL), environmentModel),
                modelByTask,
                readDouble(stored, KEY_TEMPERATURE, environmentTemperature),
                readInt(stored, KEY_MAX_OUTPUT_TOKENS, environmentMaxOutputTokens),
                Duration.ofSeconds(readInt(stored, KEY_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS)),
                readInt(stored, KEY_MAX_RETRIES, DEFAULT_MAX_RETRIES),
                readThinking(stored)
        );
    }

    private void writeApiKey(AiProviderConfigRequest request) {
        if (request.clearApiKey()) {
            settingRepository.findBySettingKey(KEY_API_KEY).ifPresent(settingRepository::delete);
            return;
        }

        // Omitted means "leave the stored key alone" — the console never receives
        // the key, so it cannot send it back to say nothing changed.
        if (!hasText(request.apiKey())) {
            return;
        }

        if (!secretCipher.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This server cannot store secrets: set AI_SETTINGS_ENCRYPTION_KEY to a "
                            + "base64-encoded 32-byte value and restart it."
            );
        }

        writeSetting(
                KEY_API_KEY,
                secretCipher.encrypt(request.apiKey().trim()),
                SettingValueType.STRING,
                "Provider API key, encrypted. Overrides the server's GEMINI_API_KEY."
        );
    }

    private void writeModelOverrides(List<AiModelOverride> overrides) {
        Map<AiTask, String> byTask = new LinkedHashMap<>();
        if (overrides != null) {
            for (AiModelOverride override : overrides) {
                String model = normalizeBlankToNull(override.model());
                if (model != null) {
                    byTask.put(override.task(), model);
                }
            }
        }

        for (AiTask task : AiTask.values()) {
            String settingKey = KEY_MODEL_PREFIX + task.name();
            String model = byTask.get(task);

            if (model == null) {
                // A cleared override is deleted rather than blanked, so the row set
                // says exactly which tasks are pinned to their own model.
                settingRepository.findBySettingKey(settingKey).ifPresent(settingRepository::delete);
                continue;
            }

            writeSetting(
                    settingKey,
                    model,
                    SettingValueType.STRING,
                    "Model used for " + task.name().toLowerCase().replace('_', ' ') + "."
            );
        }
    }

    private Map<String, SystemSetting> storedSettings() {
        Map<String, SystemSetting> byKey = new LinkedHashMap<>();
        settingRepository.findAllByCategory(CATEGORY)
                .forEach(setting -> byKey.put(setting.getSettingKey(), setting));
        return byKey;
    }

    private List<AiModelOverride> overridesFor(Map<String, SystemSetting> stored) {
        List<AiModelOverride> overrides = new ArrayList<>();
        for (AiTask task : AiTask.values()) {
            String model = readString(stored, KEY_MODEL_PREFIX + task.name());
            if (hasText(model)) {
                overrides.add(new AiModelOverride(task, model));
            }
        }
        return overrides;
    }

    private void writeSetting(
            String key,
            String value,
            SettingValueType valueType,
            String description
    ) {
        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting created = new SystemSetting();
                    created.setSettingKey(key);
                    return created;
                });

        if (Boolean.FALSE.equals(setting.getEditable())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Setting '" + key + "' is locked and cannot be changed"
            );
        }

        setting.setSettingValue(value);
        setting.setValueType(valueType);
        setting.setCategory(CATEGORY);
        setting.setDescription(description);

        settingRepository.save(setting);
    }

    private String readSecret(Map<String, SystemSetting> stored, String key) {
        return secretCipher.decrypt(readString(stored, key));
    }

    private String readString(Map<String, SystemSetting> stored, String key) {
        SystemSetting setting = stored.get(key);
        return setting == null ? null : normalizeBlankToNull(setting.getSettingValue());
    }

    private int readInt(Map<String, SystemSetting> stored, String key, int fallback) {
        String value = readString(stored, key);
        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Setting {} holds '{}', which is not a number; using {}", key, value, fallback);
            return fallback;
        }
    }

    private double readDouble(Map<String, SystemSetting> stored, String key, double fallback) {
        String value = readString(stored, key);
        if (value == null) {
            return fallback;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Setting {} holds '{}', which is not a number; using {}", key, value, fallback);
            return fallback;
        }
    }

    private AiThinking readThinking(Map<String, SystemSetting> stored) {
        String value = readString(stored, KEY_THINKING);
        if (value == null) {
            return DEFAULT_THINKING;
        }

        try {
            return AiThinking.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Setting {} holds '{}', which is not a thinking level; using {}",
                    KEY_THINKING, value, DEFAULT_THINKING);
            return DEFAULT_THINKING;
        }
    }

    private String orDefault(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    /** The provider's message is usually several causes down; the outer one says nothing useful. */
    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        String message = normalizeBlankToNull(current.getMessage());
        return message == null ? current.getClass().getSimpleName() : message;
    }
}
