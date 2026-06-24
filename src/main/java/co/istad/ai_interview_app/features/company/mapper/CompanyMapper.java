package co.istad.ai_interview_app.features.company.mapper;

import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.entity.Industry;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company company) {
        Industry industry = company.getIndustry();

        return new CompanyResponse(
                company.getId(),
                company.getRecruiterProfile().getId(),
                industry == null ? null : industry.getId(),
                industry == null ? null : industry.getName(),
                company.getName(),
                company.getDescription(),
                company.getWebsiteUrl(),
                company.getAddress(),
                company.getContactEmail(),
                company.getContactPhone(),
                company.getLogoUrl(),
                company.getBusinessRegistrationNo(),
                company.getVerificationStatus(),
                company.getStatus()
        );
    }
}
