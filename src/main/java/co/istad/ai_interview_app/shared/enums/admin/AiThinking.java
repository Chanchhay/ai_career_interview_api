package co.istad.ai_interview_app.shared.enums.admin;

/**
 * How much the model may deliberate before answering.
 *
 * <p>Thinking is what makes a modern Gemini model slow: it can add many seconds
 * to a call. Every job this platform gives the model is structured extraction
 * against an explicit schema — write these questions, score these answers,
 * split this transcript — where deliberation buys far less than it costs.
 *
 * <p>{@link #PROVIDER_DEFAULT} sends nothing and lets the model decide, which is
 * the only safe choice for a model that refuses to have thinking configured.
 */
public enum AiThinking {

    /** Send no thinking setting at all. */
    PROVIDER_DEFAULT,

    /** Disable thinking. Fastest, and rejected by a few models such as Gemini 2.5 Pro. */
    OFF,

    MINIMAL,
    LOW,
    MEDIUM,
    HIGH
}
