package co.istad.ai_interview_app.features.admin;

import co.istad.ai_interview_app.features.admin.service.AdminMetadataService;
import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.company.dto.IndustryCreateRequest;
import co.istad.ai_interview_app.features.company.dto.IndustryResponse;
import co.istad.ai_interview_app.features.company.dto.IndustryUpdateRequest;
import co.istad.ai_interview_app.features.job.dto.JobCategoryCreateRequest;
import co.istad.ai_interview_app.features.job.dto.JobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.JobCategoryUpdateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;
import co.istad.ai_interview_app.features.job.dto.SkillUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMetadataController {

    private final AdminMetadataService adminMetadataService;

    // --- Job Categories Endpoints ---

    @PostMapping("/job-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobCategoryResponse> createJobCategory(
            @Valid @RequestBody JobCategoryCreateRequest request
    ) {
        return ApiResponse.success(adminMetadataService.createJobCategory(request));
    }

    @GetMapping("/job-categories")
    public ApiResponse<List<JobCategoryResponse>> getAllJobCategories() {
        return ApiResponse.success(adminMetadataService.getAllJobCategories());
    }

    @GetMapping("/job-categories/{id}")
    public ApiResponse<JobCategoryResponse> getJobCategoryById(@PathVariable Long id) {
        return ApiResponse.success(adminMetadataService.getJobCategoryById(id));
    }

    @PutMapping("/job-categories/{id}")
    public ApiResponse<JobCategoryResponse> updateJobCategory(
            @PathVariable Long id,
            @Valid @RequestBody JobCategoryUpdateRequest request
    ) {
        return ApiResponse.success(adminMetadataService.updateJobCategory(id, request));
    }

    @DeleteMapping("/job-categories/{id}")
    public ApiResponse<Void> deleteJobCategory(@PathVariable Long id) {
        adminMetadataService.deleteJobCategory(id);
        return ApiResponse.success(null);
    }

    // --- Skills Endpoints ---

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkillResponse> createSkill(
            @Valid @RequestBody SkillCreateRequest request
    ) {
        return ApiResponse.success(adminMetadataService.createSkill(request));
    }

    @GetMapping("/skills")
    public ApiResponse<List<SkillResponse>> getAllSkills() {
        return ApiResponse.success(adminMetadataService.getAllSkills());
    }

    @GetMapping("/skills/{id}")
    public ApiResponse<SkillResponse> getSkillById(@PathVariable Long id) {
        return ApiResponse.success(adminMetadataService.getSkillById(id));
    }

    @PutMapping("/skills/{id}")
    public ApiResponse<SkillResponse> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody SkillUpdateRequest request
    ) {
        return ApiResponse.success(adminMetadataService.updateSkill(id, request));
    }

    @DeleteMapping("/skills/{id}")
    public ApiResponse<Void> deleteSkill(@PathVariable Long id) {
        adminMetadataService.deleteSkill(id);
        return ApiResponse.success(null);
    }

    // --- Industries Endpoints ---

    @PostMapping("/industries")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IndustryResponse> createIndustry(
            @Valid @RequestBody IndustryCreateRequest request
    ) {
        return ApiResponse.success(adminMetadataService.createIndustry(request));
    }

    @GetMapping("/industries")
    public ApiResponse<List<IndustryResponse>> getAllIndustries() {
        return ApiResponse.success(adminMetadataService.getAllIndustries());
    }

    @GetMapping("/industries/{id}")
    public ApiResponse<IndustryResponse> getIndustryById(@PathVariable Long id) {
        return ApiResponse.success(adminMetadataService.getIndustryById(id));
    }

    @PutMapping("/industries/{id}")
    public ApiResponse<IndustryResponse> updateIndustry(
            @PathVariable Long id,
            @Valid @RequestBody IndustryUpdateRequest request
    ) {
        return ApiResponse.success(adminMetadataService.updateIndustry(id, request));
    }

    @DeleteMapping("/industries/{id}")
    public ApiResponse<Void> deleteIndustry(@PathVariable Long id) {
        adminMetadataService.deleteIndustry(id);
        return ApiResponse.success(null);
    }
}
