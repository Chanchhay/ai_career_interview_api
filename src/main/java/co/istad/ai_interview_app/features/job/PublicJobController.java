package co.istad.ai_interview_app.features.job;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.job.dto.PublicIndustryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobResponse;
import co.istad.ai_interview_app.features.job.dto.PublicSkillResponse;
import co.istad.ai_interview_app.features.job.service.PublicJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicJobController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_JOB_SORT_PROPERTIES = Set.of(
            "id",
            "title",
            "location",
            "jobType",
            "workMode",
            "salaryMin",
            "salaryMax",
            "experienceLevel",
            "publishedAt",
            "expiredAt"
    );

    private final PublicJobService publicJobService;

    @GetMapping("/jobs")
    public ApiResponse<Page<PublicJobResponse>> findPublicJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> skillIds,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) String jobType,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        validatePublicJobsPageable(pageable);

        return ApiResponse.success(publicJobService.findPublicJobs(
                keyword,
                location,
                categoryId,
                skillIds,
                workMode,
                jobType,
                pageable
        ));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<PublicJobResponse> getPublicJob(
            @PathVariable Long jobId
    ) {
        return ApiResponse.success(publicJobService.getPublicJob(jobId));
    }

    @GetMapping("/job-categories")
    public ApiResponse<List<PublicJobCategoryResponse>> getJobCategories() {
        return ApiResponse.success(publicJobService.getJobCategories());
    }

    @GetMapping("/skills")
    public ApiResponse<List<PublicSkillResponse>> getSkills() {
        return ApiResponse.success(publicJobService.getSkills());
    }

    @GetMapping("/industries")
    public ApiResponse<List<PublicIndustryResponse>> getIndustries() {
        return ApiResponse.success(publicJobService.getIndustries());
    }

    private void validatePublicJobsPageable(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be less than or equal to " + MAX_PAGE_SIZE
            );
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_JOB_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported sort property: " + order.getProperty()
                );
            }
        }
    }
}
