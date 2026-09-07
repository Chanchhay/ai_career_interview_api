package co.istad.ai_interview_app.features.seeker.dto;

import java.util.Map;
import java.util.UUID;

/**
 * A layout a job seeker can build a resume in.
 *
 * <p>{@code templateKey} is the value that goes into {@code resumeData.templateId}
 * and decides which layout the PDF renderer uses. The numeric {@code id} is the
 * database row, which is what an administrator toggles.
 */
public record PublicResumeTemplateResponse(
        UUID id,
        String templateKey,
        String name,
        String description,
        String previewImageUrl,
        Map<String, Object> templateSchema
) {
}
