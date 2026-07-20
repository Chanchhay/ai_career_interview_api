package co.istad.ai_interview_app.features.job.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
        List<JobPostSkillResponse> skills
) {
}
