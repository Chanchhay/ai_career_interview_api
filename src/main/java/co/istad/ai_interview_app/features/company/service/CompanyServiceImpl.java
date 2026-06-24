package co.istad.ai_interview_app.features.company.service;

import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.features.company.mapper.CompanyMapper;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.company.repository.IndustryRepository;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.recruiter.repository.RecruiterProfileRepository;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final IndustryRepository industryRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final CompanyMapper companyMapper;

    @Override
    @Transactional
    public CompanyResponse createCompany(
            String keycloakUserId,
            CompanyCreateRequest request
    ) {
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserAccount_KeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Recruiter profile was not found for authenticated user"
                ));

        String businessRegistrationNo = normalize(request.businessRegistrationNo());
        if (businessRegistrationNo != null && companyRepository.existsByBusinessRegistrationNo(businessRegistrationNo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Business registration number already exists"
            );
        }

        Company company = new Company();
        company.setRecruiterProfile(recruiterProfile);
        company.setIndustry(resolveIndustry(request.industryId()));
        company.setName(normalizeRequired(request.name()));
        company.setDescription(normalize(request.description()));
        company.setWebsiteUrl(normalize(request.websiteUrl()));
        company.setAddress(normalize(request.address()));
        company.setContactEmail(normalize(request.contactEmail()));
        company.setContactPhone(normalize(request.contactPhone()));
        company.setLogoUrl(normalize(request.logoUrl()));
        company.setBusinessRegistrationNo(businessRegistrationNo);

        return companyMapper.toResponse(companyRepository.save(company));
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

    private String normalizeRequired(String value) {
        String normalizedValue = normalize(value);

        if (normalizedValue == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company name is required");
        }

        return normalizedValue;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
