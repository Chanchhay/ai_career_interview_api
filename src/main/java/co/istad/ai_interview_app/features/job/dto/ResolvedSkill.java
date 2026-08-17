package co.istad.ai_interview_app.features.job.dto;

/**
 * A skill resolved by name, with whether it had to be created.
 *
 * @param created true when this call added the skill to the shared list, false
 *                when it was already there. Only used to tell the recruiter
 *                what their upload changed.
 */
public record ResolvedSkill(
        SkillResponse skill,
        boolean created
) {
}
