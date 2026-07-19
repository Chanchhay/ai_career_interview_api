package co.istad.ai_interview_app.features.moderator;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.moderator.dto.CompanyVerificationResponse;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyListItemResponse;
import co.istad.ai_interview_app.features.moderator.service.ModeratorCompanyVerificationService;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/moderator/companies")
@RequiredArgsConstructor
public class ModeratorCompanyVerificationController {

    private final ModeratorCompanyVerificationService companyVerificationService;

    @GetMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<Page<ModeratorCompanyListItemResponse>> getCompanies(
            @RequestParam(required = false) VerificationStatus verificationStatus,
            Pageable pageable
    ) {
        return ApiResponse.success(companyVerificationService.getCompanies(verificationStatus, pageable));
    }

    @GetMapping("/{companyId}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<ModeratorCompanyDetailResponse> getCompany(
            @PathVariable Long companyId
    ) {
        return ApiResponse.success(companyVerificationService.getCompany(companyId));
    }

    @PostMapping("/{companyId}/approve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<CompanyVerificationResponse> approve(
            @PathVariable Long companyId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.approve(companyId, request));
    }

    @PostMapping("/{companyId}/reject")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<CompanyVerificationResponse> reject(
            @PathVariable Long companyId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.reject(companyId, request));
    }

    @PostMapping("/{companyId}/request-revision")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<CompanyVerificationResponse> requestRevision(
            @PathVariable Long companyId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.requestRevision(companyId, request));
    }
}
