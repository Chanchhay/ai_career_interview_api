package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.seeker.dto.PublicationRequest;
import co.istad.ai_interview_app.features.seeker.dto.PublicationResponse;
import co.istad.ai_interview_app.features.seeker.service.JobSeekerPublicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/job-seeker")
@RequiredArgsConstructor
public class JobSeekerPublicationController {

    private final JobSeekerPublicationService jobSeekerPublicationService;

    @PatchMapping("/profile/publication")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<PublicationResponse> updateProfilePublication(
            @Valid @RequestBody PublicationRequest request
    ) {
        return ApiResponse.success(jobSeekerPublicationService.updateProfilePublication(request));
    }

    @PatchMapping("/portfolios/{portfolioId}/publication")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<PublicationResponse> updatePortfolioPublication(
            @PathVariable Long portfolioId,
            @Valid @RequestBody PublicationRequest request
    ) {
        return ApiResponse.success(jobSeekerPublicationService.updatePortfolioPublication(portfolioId, request));
    }

    @PatchMapping("/resumes/{resumeId}/publication")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<PublicationResponse> updateResumePublication(
            @PathVariable Long resumeId,
            @Valid @RequestBody PublicationRequest request
    ) {
        return ApiResponse.success(jobSeekerPublicationService.updateResumePublication(resumeId, request));
    }
}
