package co.istad.ai_interview_app.features.seeker.dto;

import co.istad.ai_interview_app.shared.enums.job.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the saved-jobs page.
 *
 * <p>A save outlives the job it points at: closed and expired posts are still
 * returned, carrying {@code status} and {@code available} so the page can grey
 * the row out instead of letting it vanish without explanation.
 */
public record FavoriteJobResponse(
        UUID id,
        Instant savedAt,
        UUID jobId,
        String title,
        UUID companyId,
        String companyName,
        String location,
        String jobType,
        String workMode,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String experienceLevel,
        JobStatus status,
        Instant publishedAt,
        Instant expiredAt,
        Boolean available
) {
}
