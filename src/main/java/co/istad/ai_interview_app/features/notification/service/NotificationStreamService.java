package co.istad.ai_interview_app.features.notification.service;

import co.istad.ai_interview_app.features.notification.dto.MessageStreamEvent;
import co.istad.ai_interview_app.features.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds the open Server-Sent Events connections and pushes notifications down
 * them as they are created.
 *
 * <p><strong>Per-instance.</strong> Emitters live in this JVM's heap, so a
 * notification created on one instance reaches only the subscribers connected
 * to that instance. That is correct for a single-instance deployment and wrong
 * the moment there are two: the fix is a shared broker (Redis pub/sub, or
 * RabbitMQ) between the create and the push, not a bigger map. Until then the
 * clients also refetch on reconnect, so a missed push costs latency rather than
 * a lost notification.
 *
 * <p>Nothing here is authoritative. The database holds the notification; this
 * only saves the client a poll.
 */
@Slf4j
@Service
public class NotificationStreamService {

    /**
     * Long, but not infinite. Proxies drop idle connections eventually anyway,
     * and a bounded timeout means a client that vanished without closing
     * cleanly is reaped rather than held forever.
     */
    private static final long TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, List<SseEmitter>> emittersByUserAccountId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userAccountId) {
        return register(userAccountId, new SseEmitter(TIMEOUT_MILLIS));
    }

    /**
     * Registers an emitter against an account and opens it.
     *
     * <p>Split out of {@link #subscribe} so a test can hand in an emitter it can
     * observe — a real one needs an HTTP response behind it before it will
     * accept a single event.
     */
    SseEmitter register(Long userAccountId, SseEmitter emitter) {
        emittersByUserAccountId
                .computeIfAbsent(userAccountId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> remove(userAccountId, emitter));
        emitter.onTimeout(() -> remove(userAccountId, emitter));
        emitter.onError(throwable -> remove(userAccountId, emitter));

        /*
         * An initial event flushes the response headers immediately. Without
         * it, a proxy that buffers until the first byte would hold the whole
         * connection open before the client ever sees it open.
         */
        send(emitter, "connected", "ok");

        return emitter;
    }

    public void push(Long userAccountId, NotificationResponse notification) {
        pushEvent(userAccountId, "notification", notification);
    }

    /**
     * Tells a recipient that a conversation gained a message.
     *
     * <p>A separate event name from {@code notification} so the client can act
     * on it without decoding a notification payload, and so a muted thread still
     * updates on screen while staying quiet in the bell.
     */
    public void pushMessage(Long userAccountId, MessageStreamEvent event) {
        pushEvent(userAccountId, "message", event);
    }

    private void pushEvent(Long userAccountId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByUserAccountId.get(userAccountId);

        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            if (!send(emitter, eventName, payload)) {
                remove(userAccountId, emitter);
            }
        }
    }

    /**
     * Keeps idle connections from being closed by an intermediary that sees no
     * traffic. A comment-only line would do, but a named event lets the client
     * log it if a connection ever looks stuck.
     */
    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        emittersByUserAccountId.forEach((userAccountId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                if (!send(emitter, "heartbeat", System.currentTimeMillis())) {
                    remove(userAccountId, emitter);
                }
            }
        });
    }

    private boolean send(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
            return true;
        } catch (IOException | IllegalStateException exception) {
            // A closed browser tab lands here. Expected, not exceptional.
            log.debug("Dropping SSE emitter: {}", exception.getMessage());
            return false;
        }
    }

    private void remove(Long userAccountId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserAccountId.get(userAccountId);

        if (emitters == null) return;

        emitters.remove(emitter);

        // Do not leave an empty list behind for every user who ever connected.
        emittersByUserAccountId.computeIfPresent(
                userAccountId,
                (key, remaining) -> remaining.isEmpty() ? null : remaining
        );
    }
}
