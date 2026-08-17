package co.istad.ai_interview_app.features.recruiter.talent.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Resumes built in the app have no uploaded file, so a recruiter could only ever
 * see them if the content travels with the listing. Both are exposed: the file
 * URL drives the download button, the data renders the document inline.
 */
public record PublicResumeResponse(
        Long id,
        String title,
        Boolean isDefault,
        String resumeFileUrl,
        Map<String, Object> resumeData,
        Instant publishedAt
) {
}
