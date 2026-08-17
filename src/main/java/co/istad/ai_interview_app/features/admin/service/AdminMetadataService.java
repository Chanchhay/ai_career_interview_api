package co.istad.ai_interview_app.features.admin.service;

import co.istad.ai_interview_app.features.company.dto.IndustryCreateRequest;
import co.istad.ai_interview_app.features.company.dto.IndustryResponse;
import co.istad.ai_interview_app.features.company.dto.IndustryUpdateRequest;
import co.istad.ai_interview_app.features.job.dto.JobCategoryCreateRequest;
import co.istad.ai_interview_app.features.job.dto.JobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.JobCategoryUpdateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;
import co.istad.ai_interview_app.features.job.dto.SkillUpdateRequest;

import java.util.List;

public interface AdminMetadataService {

    // Job Category operations
    JobCategoryResponse createJobCategory(JobCategoryCreateRequest request);
    List<JobCategoryResponse> getAllJobCategories();
    JobCategoryResponse getJobCategoryById(Long id);
    JobCategoryResponse updateJobCategory(Long id, JobCategoryUpdateRequest request);
    void deleteJobCategory(Long id);

    // Skill operations
    SkillResponse createSkill(SkillCreateRequest request);
    List<SkillResponse> getAllSkills();
    SkillResponse getSkillById(Long id);
    SkillResponse updateSkill(Long id, SkillUpdateRequest request);
    void deleteSkill(Long id);

    // Industry operations
    IndustryResponse createIndustry(IndustryCreateRequest request);
    List<IndustryResponse> getAllIndustries();
    IndustryResponse getIndustryById(Long id);
    IndustryResponse updateIndustry(Long id, IndustryUpdateRequest request);
    void deleteIndustry(Long id);
}
