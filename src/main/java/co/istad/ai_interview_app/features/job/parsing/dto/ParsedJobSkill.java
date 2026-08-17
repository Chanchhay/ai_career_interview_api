package co.istad.ai_interview_app.features.job.parsing.dto;

/**
 * A skill the uploaded job description asked for, resolved to a row and
 * attached to the prefill.
 *
 * @param created true when the import added this skill to the shared list,
 *                false when it was already there. Shown to the recruiter so
 *                they can see what their upload changed.
 */
public record ParsedJobSkill(
        Long skillId,
        String name,
        String skillType,
        boolean created
) {
}
