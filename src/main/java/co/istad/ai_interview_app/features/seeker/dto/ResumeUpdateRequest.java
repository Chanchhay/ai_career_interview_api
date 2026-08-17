package co.istad.ai_interview_app.features.seeker.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record ResumeUpdateRequest(
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,
        @Size(max = 500, message = "Resume file URL must be at most 500 characters")
        String resumeFileUrl,
        Map<String, Object> resumeData
) {
}
