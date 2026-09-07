package co.istad.ai_interview_app.features.job.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * @param createdByRecruiterProfileId the recruiter who added this skill, or
 *                                    null when an admin entered it
 * @param createdByCompanyName        that recruiter's company, so the admin
 *                                    list reads as a name rather than an id;
 *                                    null for admin-entered skills, and for a
 *                                    recruiter with no company yet
 */
public record SkillResponse(
        UUID id,
        String name,
        String skillType,
        UUID createdByRecruiterProfileId,
        String createdByCompanyName,
        Instant createdAt,
        Instant updatedAt,
        UUID parentId,
        String parentName
) {
}
