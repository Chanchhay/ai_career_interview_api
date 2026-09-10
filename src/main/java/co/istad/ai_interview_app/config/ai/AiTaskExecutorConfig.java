package co.istad.ai_interview_app.config.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * The pool that runs work a caller should not wait for.
 *
 * <p>Writing an interview's questions and scoring a finished one are Gemini
 * calls measured in tens of seconds. Held inside the request they made the
 * candidate stare at a spinner for all of it, and — behind a load balancer whose
 * backend timeout is commonly 30 seconds — risked the request being cut off
 * while the work carried on and completed invisibly. Both now return as soon as
 * the session row is written, and the session's status carries the outcome.
 *
 * <p>Sized for waiting, not for computing: these threads are blocked on the
 * provider almost the whole time.
 */
@Slf4j
@Configuration
public class AiTaskExecutorConfig {

    public static final String AI_EXECUTOR = "aiTaskExecutor";

    /**
     * Runs AI work inline when false, which is how the tests stay deterministic
     * — and an operational switch for reproducing a slow interview in a request
     * where its timing can be seen directly.
     */
    @Bean(AI_EXECUTOR)
    public TaskExecutor aiTaskExecutor(
            @Value("${app.ai.async-enabled:true}") boolean asyncEnabled
    ) {
        if (!asyncEnabled) {
            log.info("AI tasks run inline: app.ai.async-enabled is false");
            return new SyncTaskExecutor();
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("ai-task-");

        /*
         * Runs the task on the calling thread once the pool and its queue are
         * both full. That is the old blocking behaviour, which is a poor
         * experience but a correct one — better than discarding an interview a
         * candidate has already sat through.
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Let in-flight scoring finish rather than stranding a session in
        // IN_PROGRESS with a transcript that will never be read.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }
}
