package co.istad.ai_interview_app.features.notification.service;

import co.istad.ai_interview_app.features.notification.dto.MessageStreamEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;

/**
 * The live stream's delivery rules.
 *
 * <p>Worth testing directly because the failure it guards against is silent: a
 * message that never arrives looks exactly like a chat nobody replied to.
 */
class NotificationStreamServiceTest {

    private static final UUID RECIPIENT = UUID.randomUUID();
    private static final UUID BYSTANDER = UUID.randomUUID();
    private static final UUID SENDER = UUID.randomUUID();
    private static final UUID OTHER_SENDER = UUID.randomUUID();
    private static final UUID CONVERSATION = UUID.randomUUID();
    private static final UUID MESSAGE = UUID.randomUUID();

    private NotificationStreamService service;

    @BeforeEach
    void setUp() {
        service = new NotificationStreamService(org.mockito.Mockito.mock(LiveWebSocketHandler.class));
    }

    @Test
    void aMessageReachesTheRecipientsOwnSubscription() throws IOException {
        RecordingEmitter recipient = new RecordingEmitter();
        service.register(RECIPIENT, recipient);

        service.pushMessage(RECIPIENT, new MessageStreamEvent(CONVERSATION, MESSAGE, SENDER, Instant.now()));

        // The initial "connected" event is sent on subscribe; the message follows.
        assertThat(recipient.events).containsExactly("connected", "message");
    }

    @Test
    void aMessageIsNotBroadcastToOtherAccounts() throws IOException {
        RecordingEmitter recipient = new RecordingEmitter();
        RecordingEmitter bystander = new RecordingEmitter();
        service.register(RECIPIENT, recipient);
        service.register(BYSTANDER, bystander);

        service.pushMessage(RECIPIENT, new MessageStreamEvent(CONVERSATION, MESSAGE, OTHER_SENDER, Instant.now()));

        assertThat(recipient.events).contains("message");
        assertThat(bystander.events).containsExactly("connected");
    }

    /** Every one of an account's open tabs sees the message, not just the first. */
    @Test
    void everyOpenConnectionForAnAccountReceivesTheMessage() throws IOException {
        RecordingEmitter firstTab = new RecordingEmitter();
        RecordingEmitter secondTab = new RecordingEmitter();
        service.register(RECIPIENT, firstTab);
        service.register(RECIPIENT, secondTab);

        service.pushMessage(RECIPIENT, new MessageStreamEvent(CONVERSATION, MESSAGE, SENDER, Instant.now()));

        assertThat(firstTab.events).contains("message");
        assertThat(secondTab.events).contains("message");
    }

    /**
     * Captures event names instead of writing to a response.
     *
     * <p>{@link SseEmitter#send} against an emitter with no HTTP response
     * attached throws, so the real thing cannot be used here without a servlet
     * container.
     */
    private static final class RecordingEmitter extends SseEmitter {

        private static final Pattern EVENT_NAME = Pattern.compile("event:(\\S+)");

        private final List<String> events = new ArrayList<>();

        @Override
        public void send(SseEventBuilder builder) {
            StringBuilder raw = new StringBuilder();
            builder.build().forEach(part -> raw.append(part.getData()));

            Matcher matcher = EVENT_NAME.matcher(raw.toString());
            while (matcher.find()) events.add(matcher.group(1));
        }
    }
}
