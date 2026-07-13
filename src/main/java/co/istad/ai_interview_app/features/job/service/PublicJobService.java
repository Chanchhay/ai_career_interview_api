package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.job.dto.PublicIndustryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobResponse;
import co.istad.ai_interview_app.features.job.dto.PublicSkillResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PublicJobService {

    Page<PublicJobResponse> findPublicJobs(
            String keyword,
            String location,
            Long categoryId,
            List<Long> skillIds,
            String workMode,
            String jobType,
            Pageable pageable
    );

    PublicJobResponse getPublicJob(Long jobId);

    List<PublicJobCategoryResponse> getJobCategories();

    List<PublicSkillResponse> getSkills();

    List<PublicIndustryResponse> getIndustries();
}
