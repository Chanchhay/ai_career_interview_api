package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.job.JobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A job as the console lists it under its company.
 *
 * Narrower than the recruiter's own view on purpose: a moderator is deciding
 * whether a posting should stay in front of candidates, which needs its title,
 * its state and when it went live — not its salary band or its question mix.
 */
public record ModeratorJobListItemResponse(
        UUID id,
        String title,
        String location,
        String jobType,
        String workMode,
        JobStatus status,
        Instant publishedAt,
        Instant expiredAt,
        Instant createdAt
) {
}
