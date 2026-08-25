package co.istad.ai_interview_app.shared.enums.admin;

/**
 * Every place the platform calls a language model. Each one can be pointed at
 * its own model — segmenting a transcript is cheap work that does not need the
 * model that scores an interview.
 */
public enum AiTask {

    /** Writes the questions for a new AI interview. */
    QUESTION_GENERATION,

    /** Scores the answers and writes the feedback and model answers. */
    ANSWER_EVALUATION,

    /** Splits a voice interview transcript back into per-question answers. */
    TRANSCRIPT_SEGMENTATION,

    /** Reads an uploaded job description into a structured job post. */
    JOB_DOCUMENT_EXTRACTION
}
