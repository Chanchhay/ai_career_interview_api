package co.istad.ai_interview_app.features.job.parsing.dto;

import co.istad.ai_interview_app.features.job.dto.JobPostSectionRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the recruiter's form receives after a PDF job description is parsed.
 *
 * <p>Nothing here has been persisted as a job post — the recruiter reviews and
 * edits these values, then saves through the normal create/update endpoints.
 * The one thing already stored is the PDF itself, at {@code sourceFileUrl},
 * which the form passes back on save.
 *
 * <p>Any field the document did not state comes back null so the form leaves it
 * empty rather than filling it with a guess. {@code expiredAt} is deliberately
 * not extracted: an "apply by" date printed in a JD is usually stale by the time
 * the post goes up.
 *
 * @param categoryId     matched job category, or null when the extracted
 *                       {@code categoryName} matched nothing
 * @param skills         every skill the document named, attached and ready to
 *                       save. Ones the shared list was missing are created as
 *                       part of the import and flagged as such
 */
public record JobDocumentParseResponse(
        String sourceFileUrl,
        String title,
        String description,
        String location,
        String jobType,
        String workMode,
        String experienceLevel,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Long categoryId,
        String categoryName,
        List<JobPostSectionRequest> sections,
        List<ParsedJobSkill> skills
) {
}
