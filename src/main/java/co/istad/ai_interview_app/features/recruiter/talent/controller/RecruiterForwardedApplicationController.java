package co.istad.ai_interview_app.features.recruiter.talent.controller;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.ForwardedApplicationResponse;
import co.istad.ai_interview_app.features.recruiter.talent.service.RecruiterForwardedApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recruiter/forwarded-applications")
@RequiredArgsConstructor
public class RecruiterForwardedApplicationController {

    private final RecruiterForwardedApplicationService forwardedApplicationService;

    @GetMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<List<ForwardedApplicationResponse>> getForwardedApplications() {
        return ApiResponse.success(forwardedApplicationService.getForwardedApplications());
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<ForwardedApplicationResponse> getForwardedApplication(
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(forwardedApplicationService.getForwardedApplication(applicationId));
    }
}
