package co.istad.ai_interview_app.features.job.dto;

import co.istad.ai_interview_app.shared.enums.job.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobPostResponse(
        Long id,
        Long companyId,
        String companyName,
        Long recruiterProfileId,
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
        JobStatus status,
        Instant publishedAt,
        Instant expiredAt,
        List<JobPostSectionResponse> sections,
        List<JobPostSkillResponse> skills
) {
}
