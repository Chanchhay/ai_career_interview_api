package co.istad.ai_interview_app.features.moderator;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.moderator.dto.CompanyIdentityVisibilityRequest;
import co.istad.ai_interview_app.features.moderator.dto.CompanyMaskedProfileRequest;
import co.istad.ai_interview_app.features.moderator.dto.CompanyVerificationResponse;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyListItemResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorJobListItemResponse;
import co.istad.ai_interview_app.features.moderator.service.ModeratorCompanyVerificationService;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderator/companies")
@RequiredArgsConstructor
public class ModeratorCompanyVerificationController {

    private final ModeratorCompanyVerificationService companyVerificationService;

    @GetMapping
    public ApiResponse<Page<ModeratorCompanyListItemResponse>> getCompanies(
            @RequestParam(required = false) VerificationStatus verificationStatus,
            Pageable pageable
    ) {
        return ApiResponse.success(companyVerificationService.getCompanies(verificationStatus, pageable));
    }

    @GetMapping("/{companyId}")
    public ApiResponse<ModeratorCompanyDetailResponse> getCompany(
            @PathVariable UUID companyId
    ) {
        return ApiResponse.success(companyVerificationService.getCompany(companyId));
    }

    /**
     * Shows or hides this company's identity from candidates.
     *
     * <p>Separate from the verification decisions below: masking is not a
     * judgement about the company, and an approved company can be masked just
     * as easily as a new one.
     */
    @PatchMapping("/{companyId}/identity-visibility")
    public ApiResponse<ModeratorCompanyDetailResponse> setIdentityVisibility(
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyIdentityVisibilityRequest request
    ) {
        return ApiResponse.success(companyVerificationService.setIdentityVisibility(companyId, request));
    }

    /**
     * Sets or clears the stand-in logo candidates see while this company is
     * masked. Send a null or blank url to take the current one down.
     */
    @PatchMapping("/{companyId}/masked-profile")
    public ApiResponse<ModeratorCompanyDetailResponse> setMaskedProfile(
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyMaskedProfileRequest request
    ) {
        return ApiResponse.success(companyVerificationService.setMaskedProfile(companyId, request));
    }

    @PostMapping("/{companyId}/approve")
    public ApiResponse<CompanyVerificationResponse> approve(
            @PathVariable UUID companyId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.approve(companyId, request));
    }

    @PostMapping("/{companyId}/reject")
    public ApiResponse<CompanyVerificationResponse> reject(
            @PathVariable UUID companyId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.reject(companyId, request));
    }

    @PostMapping("/{companyId}/request-revision")
    public ApiResponse<CompanyVerificationResponse> requestRevision(
            @PathVariable UUID companyId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.requestRevision(companyId, request));
    }

    /**
     * Suspends an approved company: its jobs stop being published and stop
     * being served to candidates. Requires a note, and 409s on a company that
     * was never approved.
     */
    @PostMapping("/{companyId}/suspend")
    public ApiResponse<CompanyVerificationResponse> suspend(
            @PathVariable UUID companyId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.suspend(companyId, request));
    }

    /** Lifts a suspension, returning the company to approved. */
    @PostMapping("/{companyId}/reinstate")
    public ApiResponse<CompanyVerificationResponse> reinstate(
            @PathVariable UUID companyId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.reinstate(companyId, request));
    }

    /**
     * The company's jobs, in every state.
     *
     * <p>Deliberately not the public catalogue: a moderator looking at a
     * company needs to see its drafts, paused and closed posts too, and the
     * public endpoint by design shows only what candidates can already find.
     */
    @GetMapping("/{companyId}/jobs")
    public ApiResponse<Page<ModeratorJobListItemResponse>> getCompanyJobs(
            @PathVariable UUID companyId,
            Pageable pageable
    ) {
        return ApiResponse.success(companyVerificationService.getCompanyJobs(companyId, pageable));
    }
}
