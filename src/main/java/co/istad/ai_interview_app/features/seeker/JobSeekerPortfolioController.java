package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectUpdateRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioUpdateRequest;
import co.istad.ai_interview_app.features.seeker.service.JobSeekerPortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-seeker/portfolios")
@RequiredArgsConstructor
public class JobSeekerPortfolioController {

    private final JobSeekerPortfolioService portfolioService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<PortfolioResponse> createPortfolio(
            @Valid @RequestBody PortfolioCreateRequest request
    ) {
        return ApiResponse.success(portfolioService.createPortfolio(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<List<PortfolioResponse>> getMyPortfolios() {
        return ApiResponse.success(portfolioService.getMyPortfolios());
    }

    @GetMapping("/{portfolioId}")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<PortfolioResponse> getMyPortfolio(@PathVariable Long portfolioId) {
        return ApiResponse.success(portfolioService.getMyPortfolio(portfolioId));
    }

    @PatchMapping("/{portfolioId}")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<PortfolioResponse> updatePortfolio(
            @PathVariable Long portfolioId,
            @Valid @RequestBody PortfolioUpdateRequest request
    ) {
        return ApiResponse.success(portfolioService.updatePortfolio(portfolioId, request));
    }

    @DeleteMapping("/{portfolioId}")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<Void> deletePortfolio(@PathVariable Long portfolioId) {
        portfolioService.deletePortfolio(portfolioId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{portfolioId}/projects")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<PortfolioProjectResponse> createProject(
            @PathVariable Long portfolioId,
            @Valid @RequestBody PortfolioProjectRequest request
    ) {
        return ApiResponse.success(portfolioService.createProject(portfolioId, request));
    }

    @PatchMapping("/{portfolioId}/projects/{projectId}")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<PortfolioProjectResponse> updateProject(
            @PathVariable Long portfolioId,
            @PathVariable Long projectId,
            @Valid @RequestBody PortfolioProjectUpdateRequest request
    ) {
        return ApiResponse.success(portfolioService.updateProject(portfolioId, projectId, request));
    }

    @DeleteMapping("/{portfolioId}/projects/{projectId}")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<Void> deleteProject(
            @PathVariable Long portfolioId,
            @PathVariable Long projectId
    ) {
        portfolioService.deleteProject(portfolioId, projectId);
        return ApiResponse.success(null);
    }
}
