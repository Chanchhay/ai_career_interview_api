package co.istad.ai_interview_app.features.job.parsing.dto;

import java.util.List;

/**
 * Raw Gemini output for an uploaded job description.
 *
 * <p>Everything here is free text or a name — never a database id. The AI has
 * no view of the category or skill tables, so resolving {@code categoryName}
 * and {@code skillNames} to rows is the service's job.
 *
 * <p>Salaries are {@code Double} rather than {@code BigDecimal} because that is
 * what the provider's structured-output schema can express; they are widened
 * before leaving the parse service.
 *
 * <p>Every field is nullable: a job description that never mentions a salary
 * should come back with nulls, not invented numbers.
 */
public record ExtractedJobDocument(
        String title,
        String description,
        String location,
        String jobType,
        String workMode,
        String experienceLevel,
        Double salaryMin,
        Double salaryMax,
        String categoryName,
        List<ExtractedJobSkill> skills,
        List<ExtractedJobSection> sections
) {
}
