package co.istad.ai_interview_app.features.recruiter.talent.dto;

import java.time.Instant;

public record PublicResumeResponse(
        Long id,
        String title,
        Boolean isDefault,
        Instant publishedAt
) {
}
