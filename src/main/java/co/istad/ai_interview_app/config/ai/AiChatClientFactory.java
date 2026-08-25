package co.istad.ai_interview_app.config.ai;

import co.istad.ai_interview_app.config.ai.dto.AiRuntimeSettings;
import co.istad.ai_interview_app.config.ai.service.AiProviderSettingsService;
import co.istad.ai_interview_app.shared.enums.admin.AiTask;
import co.istad.ai_interview_app.shared.enums.admin.AiThinking;
import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.errors.ServerException;
import com.google.genai.types.HttpOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

/**
 * Hands every AI feature a chat client built from the settings that are live
 * right now, rather than the ones the server booted with.
 *
 * <p>Clients are cached by the exact settings that produced them: a call with
 * unchanged settings reuses its client, and a saved change simply produces a
 * different cache key. Nothing has to be restarted for a new model or key to
 * take effect, and an in-flight interview keeps the client it started with.
 *
 * <p>Depends on the settings service through an {@link ObjectProvider} because
 * the service needs this factory to invalidate the cache on save — a direct
 * pair of constructor dependencies would be a cycle.
 */
@Slf4j
@Component
public class AiChatClientFactory {

    /**
     * Distinct settings combinations are few — a handful at most across a
     * deployment's life — but a console that is being fiddled with should not
     * grow the map without bound.
     */
    private static final int MAX_CACHED_CLIENTS = 16;

    private final ObjectProvider<AiProviderSettingsService> settingsProvider;
    private final Map<ClientKey, ChatClient> cache = new ConcurrentHashMap<>();

    /**
     * The settings as last read. Every AI call needs them, and re-reading four
     * rows and decrypting a key on each one is latency spent to learn something
     * that only changes when an administrator saves — which clears this.
     */
    private volatile AiRuntimeSettings snapshot;

    public AiChatClientFactory(ObjectProvider<AiProviderSettingsService> settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    /** The client for one kind of work, on that task's model if an admin pinned one. */
    public ChatClient forTask(AiTask task) {
        AiRuntimeSettings settings = settings();
        return client(keyFor(settings.apiKey(), settings.modelFor(task), settings));
    }

    private AiRuntimeSettings settings() {
        AiRuntimeSettings current = snapshot;
        if (current == null) {
            // Two callers racing here both read the same rows and store the same
            // result, so the duplicate read is harmless and cheaper than locking.
            current = settingsProvider.getObject().current();
            snapshot = current;
        }
        return current;
    }

    /** Used by the connection test, which may be checking a key that is not saved yet. */
    public ChatClient clientFor(String apiKey, String model, AiRuntimeSettings settings) {
        return client(keyFor(apiKey, model, settings));
    }

    /** Drops the cached clients so the next call picks up settings just saved. */
    public void settingsChanged() {
        snapshot = null;
        cache.clear();
    }

    private ChatClient client(ClientKey key) {
        if (cache.size() > MAX_CACHED_CLIENTS) {
            cache.clear();
        }

        return cache.computeIfAbsent(key, this::build);
    }

    private ClientKey keyFor(String apiKey, String model, AiRuntimeSettings settings) {
        return new ClientKey(
                apiKey,
                model,
                settings.temperature(),
                settings.maxOutputTokens(),
                settings.timeout().toMillis(),
                settings.maxRetries(),
                settings.thinking()
        );
    }

    private ChatClient build(ClientKey key) {
        if (!hasText(key.apiKey())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No AI API key is configured. Set one in the admin console, or start the "
                            + "server with GEMINI_API_KEY."
            );
        }

        log.info(
                "Building AI chat client model={} timeoutMs={} maxRetries={} thinking={}",
                key.model(), key.timeoutMillis(), key.maxRetries(), key.thinking()
        );

        Client genAiClient = Client.builder()
                .apiKey(key.apiKey())
                .httpOptions(HttpOptions.builder()
                        .timeout(Math.toIntExact(key.timeoutMillis()))
                        .build())
                .build();

        // Written as statements rather than a chain: the inherited setters return
        // the base builder type, so chaining them would lose the Google options.
        GoogleGenAiChatOptions.Builder options = GoogleGenAiChatOptions.builder();
        options.model(key.model());
        options.temperature(key.temperature());
        options.maxOutputTokens(key.maxOutputTokens());
        applyThinking(options, key.thinking());

        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(genAiClient)
                .options(options.build())
                .retryTemplate(new RetryTemplate(retryPolicy(key.maxRetries())))
                .build();

        return ChatClient.create(chatModel);
    }

    /**
     * Thinking is the difference between a reply in seconds and one in tens of
     * seconds. OFF is expressed as a zero budget, which is what the 2.5 family
     * understands; the named levels use the newer control the 3.x family reads.
     * PROVIDER_DEFAULT sets neither, which is the only thing a model that
     * refuses to be configured will accept.
     */
    private void applyThinking(GoogleGenAiChatOptions.Builder options, AiThinking thinking) {
        switch (thinking) {
            case PROVIDER_DEFAULT -> {
                // Deliberately nothing.
            }
            case OFF -> options.thinkingBudget(0);
            case MINIMAL -> options.thinkingLevel(GoogleGenAiThinkingLevel.MINIMAL);
            case LOW -> options.thinkingLevel(GoogleGenAiThinkingLevel.LOW);
            case MEDIUM -> options.thinkingLevel(GoogleGenAiThinkingLevel.MEDIUM);
            case HIGH -> options.thinkingLevel(GoogleGenAiThinkingLevel.HIGH);
        }
    }

    /**
     * Retries what a second attempt could plausibly fix — the provider being
     * briefly unwell, or the network dropping — and nothing else.
     *
     * <p>4xx answers are deliberately excluded. A rejected key, an unknown model
     * and an exhausted billing quota are all client errors, and none of them
     * become true a second later; retrying them only turns one clear failure
     * into several slow ones. A genuine rate-limit 429 would want a retry, but
     * not one a second later, so it is left out with the rest and surfaces
     * quickly instead.
     */
    private RetryPolicy retryPolicy(int maxRetries) {
        return RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(Duration.ofSeconds(1))
                .excludes(ClientException.class)
                .includes(ServerException.class, GenAiIOException.class)
                .build();
    }

    /**
     * Everything that changes what a client does. The API key is part of it, so
     * rotating the key cannot leave the old one serving from cache.
     */
    private record ClientKey(
            String apiKey,
            String model,
            Double temperature,
            Integer maxOutputTokens,
            long timeoutMillis,
            Integer maxRetries,
            AiThinking thinking
    ) {
    }
}
