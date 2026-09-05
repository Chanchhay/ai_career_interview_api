package co.istad.ai_interview_app.shared.enums.interview;

/**
 * Where a guest interview's questions come from.
 *
 * <p>Set once by an administrator and applied to every guest interview,
 * whichever job the guest picked. A guest is a stranger being shown what the
 * platform does, so the choice is about what makes a good demonstration rather
 * than about any one job's hiring needs.
 */
public enum GuestQuestionSource {

    /**
     * Use whatever the chosen job is already set to — its written questions and
     * its own manual/AI mode. The default: a guest then sees exactly the
     * interview a real candidate for that job would sit.
     */
    FOLLOW_JOB,

    /**
     * Only the questions an administrator wrote for the job. Predictable, and
     * it spends nothing on generation — but a job with no written questions
     * cannot be interviewed for at all, which the guest is told plainly.
     */
    WRITTEN_ONLY,

    /** Always let the AI write the whole set, ignoring any written questions. */
    ALWAYS_GENERATE
}
