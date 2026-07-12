package co.istad.ai_interview_app.features.company;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyUpdateRequest;
import co.istad.ai_interview_app.features.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruiter/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<CompanyResponse> createCompany(
            @Valid @RequestBody CompanyCreateRequest request
    ) {
        return ApiResponse.success(
                companyService.createCompany(request)
        );
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<CompanyResponse> getMyCompany() {
        return ApiResponse.success(
                companyService.getMyCompany()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<CompanyResponse> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyUpdateRequest request
    ) {
        return ApiResponse.success(
                companyService.updateCompany(id, request)
        );
    }
}
