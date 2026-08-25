package co.istad.ai_interview_app.features.seeker.dto;

import co.istad.ai_interview_app.shared.enums.seeker.ResumeSourceType;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;

import java.time.Instant;
import java.util.Map;

public record ResumeResponse(
        Long id,
        String title,
        String resumeFileUrl,
        Map<String, Object> resumeData,
        Boolean isDefault,
        VisibilityStatus visibility,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        /** Whether this is rendered from resumeData or a file the seeker brought. */
        ResumeSourceType sourceType,
        /** When resumeFileUrl was last rendered. Null for uploads and for drafts. */
        Instant generatedAt,
        Integer fileVersion,
        /** True when there is a file to download at all. */
        Boolean hasFile
) {
}
