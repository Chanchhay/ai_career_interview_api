package co.istad.ai_interview_app.features.seeker.dto;

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
        Instant updatedAt
) {
}
