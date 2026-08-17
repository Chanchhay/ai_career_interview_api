package co.istad.ai_interview_app.features.job.dto;

import java.time.Instant;

/**
 * @param createdByRecruiterProfileId the recruiter who added this skill, or
 *                                    null when an admin entered it
 * @param createdByCompanyName        that recruiter's company, so the admin
 *                                    list reads as a name rather than an id;
 *                                    null for admin-entered skills, and for a
 *                                    recruiter with no company yet
 */
public record SkillResponse(
        Long id,
        String name,
        String skillType,
        Long createdByRecruiterProfileId,
        String createdByCompanyName,
        Instant createdAt,
        Instant updatedAt
) {
}
