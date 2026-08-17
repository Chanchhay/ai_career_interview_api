package co.istad.ai_interview_app.features.job;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.job.dto.JobPostRequest;
import co.istad.ai_interview_app.features.job.dto.JobPostResponse;
import co.istad.ai_interview_app.features.job.service.RecruiterJobPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recruiter/jobs")
@RequiredArgsConstructor
public class RecruiterJobPostController {

    private final RecruiterJobPostService recruiterJobPostService;

    @PostMapping
    public ApiResponse<JobPostResponse> createJobDraft(
            @Valid @RequestBody JobPostRequest request
    ) {
        return ApiResponse.success(
                recruiterJobPostService.createJobDraft(request)
        );
    }

    @GetMapping
    public ApiResponse<List<JobPostResponse>> getMyJobs() {
        return ApiResponse.success(
                recruiterJobPostService.getMyJobs()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<JobPostResponse> getMyJob(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                recruiterJobPostService.getMyJob(id)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<JobPostResponse> updateMyJob(
            @PathVariable Long id,
            @Valid @RequestBody JobPostRequest request
    ) {
        return ApiResponse.success(
                recruiterJobPostService.updateMyJob(id, request)
        );
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<JobPostResponse> publishMyJob(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                recruiterJobPostService.publishMyJob(id)
        );
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<JobPostResponse> pauseMyJob(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                recruiterJobPostService.pauseMyJob(id)
        );
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<JobPostResponse> resumeMyJob(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                recruiterJobPostService.resumeMyJob(id)
        );
    }

    @PostMapping("/{id}/close")
    public ApiResponse<JobPostResponse> closeMyJob(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                recruiterJobPostService.closeMyJob(id)
        );
    }
}
