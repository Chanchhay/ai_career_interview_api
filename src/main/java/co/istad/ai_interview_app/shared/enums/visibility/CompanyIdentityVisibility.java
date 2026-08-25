package co.istad.ai_interview_app.shared.enums.visibility;

/**
 * Whether candidates are told which company is behind a job.
 *
 * <p>Set by an administrator on the company, and it covers every job that
 * company posts. It changes nothing for the recruiter who owns the company, for
 * moderators, or for finance — all of them are working on the company's behalf
 * and need to know who it is. Only the candidate-facing side masks.
 */
public enum CompanyIdentityVisibility {

    /** The company is named wherever it appears. The default. */
    VISIBLE,

    /**
     * Candidates see a placeholder instead of the name, and no link back to the
     * company. Nothing reveals it later: masked stays masked on every
     * candidate-facing response until an administrator changes this back.
     */
    MASKED
}
