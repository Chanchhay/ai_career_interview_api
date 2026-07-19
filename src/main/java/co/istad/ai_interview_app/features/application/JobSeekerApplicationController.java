package co.istad.ai_interview_app.features.application;

import co.istad.ai_interview_app.features.application.dto.JobApplicationCreateRequest;
import co.istad.ai_interview_app.features.application.dto.JobApplicationResponse;
import co.istad.ai_interview_app.features.application.service.JobSeekerApplicationService;
import co.istad.ai_interview_app.features.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-seeker")
@RequiredArgsConstructor
public class JobSeekerApplicationController {

    private final JobSeekerApplicationService applicationService;

    @PostMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<JobApplicationResponse> apply(
            @PathVariable Long jobId,
            @Valid @RequestBody JobApplicationCreateRequest request
    ) {
        return ApiResponse.success(applicationService.apply(jobId, request));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<List<JobApplicationResponse>> getMyApplications() {
        return ApiResponse.success(applicationService.getMyApplications());
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<JobApplicationResponse> getMyApplication(
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(applicationService.getMyApplication(applicationId));
    }

    @PostMapping("/applications/{applicationId}/withdraw")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<JobApplicationResponse> withdraw(
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(applicationService.withdraw(applicationId));
    }
}
