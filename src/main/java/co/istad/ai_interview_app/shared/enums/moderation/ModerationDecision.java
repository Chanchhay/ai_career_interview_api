package co.istad.ai_interview_app.shared.enums.moderation;

public enum ModerationDecision {
    APPROVED,
    REJECTED,
    NEEDS_REVISION,
    /** An approved company's rights withdrawn, and given back again. */
    SUSPENDED,
    REINSTATED
}