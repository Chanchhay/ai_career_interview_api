package co.istad.ai_interview_app.shared.enums.interview;

public enum InterviewStatus {
    PREPARING,
    READY,
    PENDING,
    IN_PROGRESS,

    /**
     * Answers are all in and the model is marking them.
     *
     * <p>Its own state rather than a flag on IN_PROGRESS because scoring runs
     * off the request thread: without it a reload would re-offer "finish" and
     * start a second, duplicate evaluation, and the screen could not tell a
     * candidate still answering from one waiting on a result.
     */
    SCORING,

    COMPLETED,
    FAILED,
    CANCELLED
}
