package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.seeker.dto.FavoriteJobResponse;
import co.istad.ai_interview_app.features.seeker.service.JobSeekerFavoriteJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/job-seeker/favorite-jobs")
@RequiredArgsConstructor
public class JobSeekerFavoriteJobController {

    private static final int MAX_PAGE_SIZE = 100;

    private final JobSeekerFavoriteJobService jobSeekerFavoriteJobService;

    @GetMapping
    public ApiResponse<Page<FavoriteJobResponse>> findFavoriteJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be less than or equal to " + MAX_PAGE_SIZE
            );
        }

        return ApiResponse.success(jobSeekerFavoriteJobService.findFavoriteJobs(pageable));
    }

    /**
     * Saving is keyed on the job rather than on a favorite id, so the button
     * that saves and the button that unsaves address the same URL. Idempotent:
     * a repeat save returns the existing row with 200 rather than duplicating.
     */
    @PostMapping("/{jobId}")
    public ApiResponse<FavoriteJobResponse> saveFavoriteJob(
            @PathVariable Long jobId
    ) {
        return ApiResponse.success(jobSeekerFavoriteJobService.saveFavoriteJob(jobId));
    }

    @DeleteMapping("/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavoriteJob(
            @PathVariable Long jobId
    ) {
        jobSeekerFavoriteJobService.removeFavoriteJob(jobId);
    }
}
