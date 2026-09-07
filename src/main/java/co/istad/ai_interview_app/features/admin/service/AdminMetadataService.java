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
import java.util.UUID;

public interface AdminMetadataService {

    // Job Category operations
    JobCategoryResponse createJobCategory(JobCategoryCreateRequest request);
    List<JobCategoryResponse> getAllJobCategories();
    JobCategoryResponse getJobCategoryById(UUID id);
    JobCategoryResponse updateJobCategory(UUID id, JobCategoryUpdateRequest request);
    void deleteJobCategory(UUID id);

    // Skill operations
    SkillResponse createSkill(SkillCreateRequest request);
    List<SkillResponse> getAllSkills();
    SkillResponse getSkillById(UUID id);
    SkillResponse updateSkill(UUID id, SkillUpdateRequest request);
    void deleteSkill(UUID id);

    // Industry operations
    IndustryResponse createIndustry(IndustryCreateRequest request);
    List<IndustryResponse> getAllIndustries();
    IndustryResponse getIndustryById(UUID id);
    IndustryResponse updateIndustry(UUID id, IndustryUpdateRequest request);
    void deleteIndustry(UUID id);
}
