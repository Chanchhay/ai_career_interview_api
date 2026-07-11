package co.istad.ai_interview_app.features.company.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyUpdateRequest;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.features.company.mapper.CompanyMapper;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.company.repository.IndustryRepository;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.recruiter.service.AuthenticatedRecruiterProfileResolver;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final IndustryRepository industryRepository;
    private final AuthenticatedRecruiterProfileResolver recruiterProfileResolver;
    private final CompanyMapper companyMapper;

    @Override
    @Transactional
    public CompanyResponse createCompany(
            CompanyCreateRequest request
    ) {
        RecruiterProfile recruiterProfile = recruiterProfileResolver.resolve();

        if (companyRepository.existsByRecruiterProfile_Id(recruiterProfile.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Recruiter already has a company profile"
            );
        }

        String businessRegistrationNo = normalizeBlankToNull(request.businessRegistrationNo());
        if (businessRegistrationNo != null && companyRepository.existsByBusinessRegistrationNo(businessRegistrationNo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Business registration number already exists"
            );
        }

        Company company = companyMapper.toEntity(
                request,
                resolveIndustry(request.industryId())
        );
        company.setRecruiterProfile(recruiterProfile);

        return companyMapper.toResponse(companyRepository.save(company));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany() {
        Company company = companyRepository.findByRecruiterProfile_UserAccount_KeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Company profile was not found for authenticated recruiter"
                ));

        return companyMapper.toResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(
            Long id,
            CompanyUpdateRequest request
    ) {
        Company company = companyRepository.findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(
                        id,
                        AuthUtils.extractUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Company profile was not found for authenticated recruiter"
                ));

        String businessRegistrationNo = normalizeBlankToNull(request.businessRegistrationNo());
        if (businessRegistrationNo != null
                && companyRepository.existsByBusinessRegistrationNoAndIdNot(businessRegistrationNo, company.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Business registration number already exists"
            );
        }

        companyMapper.updateEntity(
                company,
                request,
                resolveIndustry(request.industryId())
        );

        return companyMapper.toResponse(company);
    }

    private Industry resolveIndustry(Long industryId) {
        if (industryId == null) {
            return null;
        }

        Industry industry = industryRepository.findById(industryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Industry was not found"
                ));

        if (industry.getStatus() != ProfileStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Industry is not active");
        }

        return industry;
    }
}
