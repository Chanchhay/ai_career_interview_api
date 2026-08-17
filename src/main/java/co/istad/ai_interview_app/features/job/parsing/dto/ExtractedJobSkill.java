package co.istad.ai_interview_app.features.job.parsing.dto;

/**
 * A skill the document asks for, as Gemini read it.
 *
 * <p>{@code skillType} is a guess used only when the name turns out to be new
 * to the skills table and a recruiter chooses to create it; an existing skill
 * keeps whatever type it already has.
 */
public record ExtractedJobSkill(
        String name,
        String skillType
) {
}
