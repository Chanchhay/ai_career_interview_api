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

/**
 * The live stream's delivery rules.
 *
 * <p>Worth testing directly because the failure it guards against is silent: a
 * message that never arrives looks exactly like a chat nobody replied to.
 */
class NotificationStreamServiceTest {

    private NotificationStreamService service;

    @BeforeEach
    void setUp() {
        service = new NotificationStreamService();
    }

    @Test
    void aMessageReachesTheRecipientsOwnSubscription() throws IOException {
        RecordingEmitter recipient = new RecordingEmitter();
        service.register(1L, recipient);

        service.pushMessage(1L, new MessageStreamEvent(7L, 42L, 2L, Instant.now()));

        // The initial "connected" event is sent on subscribe; the message follows.
        assertThat(recipient.events).containsExactly("connected", "message");
    }

    @Test
    void aMessageIsNotBroadcastToOtherAccounts() throws IOException {
        RecordingEmitter recipient = new RecordingEmitter();
        RecordingEmitter bystander = new RecordingEmitter();
        service.register(1L, recipient);
        service.register(2L, bystander);

        service.pushMessage(1L, new MessageStreamEvent(7L, 42L, 3L, Instant.now()));

        assertThat(recipient.events).contains("message");
        assertThat(bystander.events).containsExactly("connected");
    }

    /** Every one of an account's open tabs sees the message, not just the first. */
    @Test
    void everyOpenConnectionForAnAccountReceivesTheMessage() throws IOException {
        RecordingEmitter firstTab = new RecordingEmitter();
        RecordingEmitter secondTab = new RecordingEmitter();
        service.register(1L, firstTab);
        service.register(1L, secondTab);

        service.pushMessage(1L, new MessageStreamEvent(7L, 42L, 2L, Instant.now()));

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
