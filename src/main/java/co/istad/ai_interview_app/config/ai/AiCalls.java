package co.istad.ai_interview_app.config.ai;

import co.istad.ai_interview_app.shared.enums.admin.AiTask;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Times every call to a language model.
 *
 * <p>Exists because "the AI is slow" was previously unanswerable: three
 * different tasks call Gemini, scoring calls it twice in a row, and nothing
 * recorded which one took the time. A line per call turns that into a question
 * the logs answer — and shows immediately whether a slow result is one long call
 * or a retry storm behind a short one.
 */
@Slf4j
public final class AiCalls {

    private static final Logger LOG = LoggerFactory.getLogger(AiCalls.class);

    /** Anything past this is worth noticing even when it eventually succeeds. */
    private static final long SLOW_MILLIS = 15_000;

    private AiCalls() {
    }

    public static <T> T timed(AiTask task, Supplier<T> call) {
        long startedAt = System.nanoTime();

        try {
            T result = call.get();
            long millis = (System.nanoTime() - startedAt) / 1_000_000;

            if (millis >= SLOW_MILLIS) {
                LOG.warn("AI task={} took {} ms", task, millis);
            } else {
                LOG.info("AI task={} took {} ms", task, millis);
            }

            return result;
        } catch (RuntimeException ex) {
            long millis = (System.nanoTime() - startedAt) / 1_000_000;
            LOG.warn("AI task={} failed after {} ms: {}", task, millis, ex.toString());
            throw ex;
        }
    }
}
