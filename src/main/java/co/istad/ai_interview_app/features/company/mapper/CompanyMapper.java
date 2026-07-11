package co.istad.ai_interview_app.features.company.mapper;

import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyUpdateRequest;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.entity.Industry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "recruiterProfile", ignore = true)
    @Mapping(target = "industry", source = "industry")
    @Mapping(target = "name", source = "request.name", qualifiedByName = "normalize")
    @Mapping(target = "description", source = "request.description", qualifiedByName = "normalize")
    @Mapping(target = "websiteUrl", source = "request.websiteUrl", qualifiedByName = "normalize")
    @Mapping(target = "address", source = "request.address", qualifiedByName = "normalize")
    @Mapping(target = "contactEmail", source = "request.contactEmail", qualifiedByName = "normalize")
    @Mapping(target = "contactPhone", source = "request.contactPhone", qualifiedByName = "normalize")
    @Mapping(target = "logoUrl", source = "request.logoUrl", qualifiedByName = "normalize")
    @Mapping(target = "businessRegistrationNo", source = "request.businessRegistrationNo", qualifiedByName = "normalize")
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "status", ignore = true)
    Company toEntity(
            CompanyCreateRequest request,
            Industry industry
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "recruiterProfile", ignore = true)
    @Mapping(target = "industry", source = "industry")
    @Mapping(target = "name", source = "request.name", qualifiedByName = "normalize")
    @Mapping(target = "description", source = "request.description", qualifiedByName = "normalize")
    @Mapping(target = "websiteUrl", source = "request.websiteUrl", qualifiedByName = "normalize")
    @Mapping(target = "address", source = "request.address", qualifiedByName = "normalize")
    @Mapping(target = "contactEmail", source = "request.contactEmail", qualifiedByName = "normalize")
    @Mapping(target = "contactPhone", source = "request.contactPhone", qualifiedByName = "normalize")
    @Mapping(target = "logoUrl", source = "request.logoUrl", qualifiedByName = "normalize")
    @Mapping(target = "businessRegistrationNo", source = "request.businessRegistrationNo", qualifiedByName = "normalize")
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(
            @MappingTarget Company company,
            CompanyUpdateRequest request,
            Industry industry
    );

    @Mapping(target = "recruiterProfileId", source = "recruiterProfile.id")
    @Mapping(target = "industryId", source = "industry.id")
    @Mapping(target = "industryName", source = "industry.name")
    CompanyResponse toResponse(Company company);

    @Named("normalize")
    default String normalize(String value) {
        return normalizeBlankToNull(value);
    }
}
