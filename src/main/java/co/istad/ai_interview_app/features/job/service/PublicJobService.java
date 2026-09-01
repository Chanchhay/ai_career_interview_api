package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.job.dto.PublicIndustryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobFacetsResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobFilter;
import co.istad.ai_interview_app.features.job.dto.PublicJobResponse;
import co.istad.ai_interview_app.features.job.dto.PublicSkillResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PublicJobService {

    Page<PublicJobResponse> findPublicJobs(PublicJobFilter filter, Pageable pageable);

    /**
     * What the same search could still be narrowed by, and by how much. Takes
     * the filter the listing was asked for, so the sidebar describes the result
     * on screen rather than the whole board.
     */
    PublicJobFacetsResponse findPublicJobFacets(PublicJobFilter filter);

    PublicJobResponse getPublicJob(Long jobId);

    List<PublicJobCategoryResponse> getJobCategories();

    List<PublicSkillResponse> getSkills();

    List<PublicIndustryResponse> getIndustries();
}
