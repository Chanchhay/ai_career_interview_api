package co.istad.ai_interview_app.features.job.dto;

import co.istad.ai_interview_app.shared.enums.job.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobPostResponse(
        UUID id,
        UUID companyId,
        String companyName,
        UUID recruiterProfileId,
        UUID categoryId,
        String categoryName,
        String title,
        String description,
        String location,
        String jobType,
        String workMode,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String experienceLevel,
        JobStatus status,
        Instant publishedAt,
        Instant expiredAt,
        String sourceFileUrl,
        List<JobPostSectionResponse> sections,
        List<JobPostSkillResponse> skills
) {
}
