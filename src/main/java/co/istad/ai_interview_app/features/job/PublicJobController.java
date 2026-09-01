package co.istad.ai_interview_app.features.job;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.job.dto.PublicIndustryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobFacetsResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobFilter;
import co.istad.ai_interview_app.features.job.dto.PublicJobResponse;
import co.istad.ai_interview_app.features.job.dto.PublicSkillResponse;
import co.istad.ai_interview_app.features.job.service.PublicJobService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    /**
     * The job board's listing. Every filter is optional and they compose with
     * AND; the list-valued ones (repeat the parameter, or send it comma
     * separated) match any of their values, which is how the checkbox groups in
     * the sidebar are meant to arrive.
     */
    @GetMapping("/jobs")
    public ApiResponse<Page<PublicJobResponse>> findPublicJobs(
            PublicJobFilterParams params,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        validatePublicJobsPageable(pageable);

        return ApiResponse.success(publicJobService.findPublicJobs(params.toFilter(), pageable));
    }

    /**
     * The options the sidebar should offer for that same search, each with the
     * number of jobs behind it. Takes exactly the parameters
     * {@link #findPublicJobs} takes, minus the paging.
     */
    @GetMapping("/jobs/facets")
    public ApiResponse<PublicJobFacetsResponse> findPublicJobFacets(PublicJobFilterParams params) {
        return ApiResponse.success(publicJobService.findPublicJobFacets(params.toFilter()));
    }

    /**
     * The listing's query string. Bound as an object so that the listing and
     * its facets cannot drift apart: one place defines what can be filtered on
     * and how it is validated.
     */
    @Getter
    @Setter
    public static class PublicJobFilterParams {

        private String keyword;
        private String location;
        private List<Long> categoryId;
        private List<Long> skillIds;
        private List<String> workMode;
        private List<String> jobType;
        private List<String> experienceLevel;
        private BigDecimal salaryMin;
        private BigDecimal salaryMax;
        /** Only jobs published within this many days. */
        private Integer postedWithinDays;

        PublicJobFilter toFilter() {
            validateSalaryRange();

            return new PublicJobFilter(
                    keyword,
                    location,
                    categoryId,
                    skillIds,
                    workMode,
                    jobType,
                    experienceLevel,
                    salaryMin,
                    salaryMax,
                    postedAfter()
            );
        }

        private Instant postedAfter() {
            if (postedWithinDays == null) {
                return null;
            }

            if (postedWithinDays < 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "postedWithinDays must be at least 1"
                );
            }

            return Instant.now().minus(postedWithinDays, ChronoUnit.DAYS);
        }

        private void validateSalaryRange() {
            if (isNegative(salaryMin) || isNegative(salaryMax)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Salary filters must not be negative"
                );
            }

            if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "salaryMin must be less than or equal to salaryMax"
                );
            }
        }

        private boolean isNegative(BigDecimal value) {
            return value != null && value.signum() < 0;
        }
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
