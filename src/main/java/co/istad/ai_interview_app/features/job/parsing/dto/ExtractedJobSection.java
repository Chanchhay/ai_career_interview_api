package co.istad.ai_interview_app.features.job.parsing.dto;

import co.istad.ai_interview_app.shared.enums.job.JobPostSectionType;

/**
 * One themed block of the job description as Gemini split it out of the PDF.
 *
 * @param sectionType     which of the known section slots this block belongs in
 * @param title           heading to show above the block
 * @param contentMarkdown block body, as markdown bullets or paragraphs
 */
public record ExtractedJobSection(
        JobPostSectionType sectionType,
        String title,
        String contentMarkdown
) {
}
