package co.istad.ai_interview_app.features.job.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A published job as the public site renders it.
 *
 * <p>{@code isFavorite} is the one field that depends on who is asking: true or
 * false for a signed-in job seeker, {@code null} for anyone else, including
 * anonymous visitors and recruiters. Because of it these responses are
 * per-caller and must not be shared by a cache that ignores the Authorization
 * header.
 */
public record PublicJobResponse(
        Long id,
        Long companyId,
        String companyName,
        Long categoryId,
        String categoryName,
        String title,
        String description,
        String location,
        String jobType,
        String workMode,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String experienceLevel,
        Instant publishedAt,
        Instant expiredAt,
        List<JobPostSectionResponse> sections,
        List<JobPostSkillResponse> skills,
        Boolean isFavorite
) {
}
