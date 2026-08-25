package co.istad.ai_interview_app.features.notification.dto;

import java.time.Instant;

/**
 * A new message, pushed down the live stream so an open chat updates itself.
 *
 * <p>Separate from the notification it may also produce, because the two answer
 * different questions. A notification asks for the recipient's attention and is
 * suppressed when they have muted the thread; this says the transcript changed,
 * and is sent to every recipient regardless of muting — someone reading a muted
 * thread should still watch it fill in. Muting is about interruption, not about
 * being shown stale data.
 *
 * <p>Carries ids rather than the message body: the client refetches the
 * conversation, so nothing here has to be trusted or merged, and a dropped
 * event costs a refetch rather than a missing message.
 */
public record MessageStreamEvent(
        Long conversationId,
        Long messageId,
        Long senderUserAccountId,
        Instant sentAt
) {
}
