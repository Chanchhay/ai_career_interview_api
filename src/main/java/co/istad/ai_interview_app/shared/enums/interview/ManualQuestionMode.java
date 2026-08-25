package co.istad.ai_interview_app.shared.enums.interview;

/**
 * What a job's hand-written interview questions do to AI generation.
 *
 * <p>Only consulted when the job actually has hand-written questions. A job with
 * none is generated exactly as it always was, whatever this says — which is why
 * adding the first question to a job cannot silently change how every other job
 * behaves.
 */
public enum ManualQuestionMode {

    /**
     * Ask exactly the written questions and nothing else. The AI is not called
     * for this job at all, so the interview is entirely predictable — and
     * entirely the author's responsibility.
     */
    MANUAL_ONLY,

    /**
     * Ask the written questions first, then let the AI fill the rest of the
     * configured question count.
     *
     * <p>The default, because it is the safe answer to "somebody added one
     * must-ask question": the interview keeps its usual length instead of
     * shrinking to that single question.
     */
    MANUAL_PLUS_AI
}
