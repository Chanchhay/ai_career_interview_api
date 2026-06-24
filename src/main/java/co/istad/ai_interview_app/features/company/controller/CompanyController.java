package co.istad.ai_interview_app.features.company.controller;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.common.security.AuthenticatedUserResolver;
import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruiter/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<CompanyResponse> createCompany(
            Authentication authentication,
            @Valid @RequestBody CompanyCreateRequest request
    ) {
        return ApiResponse.success(
                companyService.createCompany(authenticatedUserResolver.resolveSubject(authentication), request)
        );
    }
}
