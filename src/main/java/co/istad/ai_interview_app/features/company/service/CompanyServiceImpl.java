package co.istad.ai_interview_app.features.company.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyDocumentRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyDocumentResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyUpdateRequest;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.entity.CompanyDocument;
import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.features.company.mapper.CompanyMapper;
import co.istad.ai_interview_app.features.company.repository.CompanyDocumentRepository;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.company.repository.IndustryRepository;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.recruiter.service.AuthenticatedRecruiterProfileResolver;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private static final Set<String> REQUIRED_DOCUMENT_TYPES = Set.of("BUSINESS_REGISTRATION");

    private final CompanyRepository companyRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
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

    @Override
    @Transactional
    public CompanyDocumentResponse addDocument(Long companyId, CompanyDocumentRequest request) {
        RecruiterProfile recruiterProfile = recruiterProfileResolver.resolve();
        Company company = resolveMyCompany(companyId);

        CompanyDocument document = new CompanyDocument();
        document.setCompany(company);
        document.setUploadedByRecruiterProfile(recruiterProfile);
        document.setDocumentType(normalizeDocumentType(request.documentType()));
        document.setDocumentUrl(normalizeBlankToNull(request.documentUrl()));
        document.setStatus(ProfileStatus.ACTIVE);

        return toDocumentResponse(companyDocumentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDocumentResponse> getDocuments(Long companyId) {
        resolveMyCompany(companyId);

        return companyDocumentRepository.findAllByCompany_IdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteDocument(Long companyId, Long documentId) {
        CompanyDocument document = companyDocumentRepository
                .findByIdAndCompany_IdAndCompany_RecruiterProfile_UserAccount_KeycloakUserId(
                        documentId,
                        companyId,
                        AuthUtils.extractUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Company document was not found for authenticated recruiter"
                ));

        companyDocumentRepository.delete(document);
    }

    @Override
    @Transactional
    public CompanyResponse submitVerification(Long companyId) {
        Company company = resolveMyCompany(companyId);
        validateRequiredDocuments(company.getId());

        company.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        company.setStatus(ProfileStatus.PENDING);

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

    private Company resolveMyCompany(Long companyId) {
        return companyRepository.findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(
                        companyId,
                        AuthUtils.extractUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Company profile was not found for authenticated recruiter"
                ));
    }

    private void validateRequiredDocuments(Long companyId) {
        Set<String> uploadedTypes = companyDocumentRepository.findAllByCompany_IdOrderByCreatedAtDesc(companyId)
                .stream()
                .filter(document -> document.getStatus() == ProfileStatus.ACTIVE)
                .map(document -> normalizeDocumentType(document.getDocumentType()))
                .collect(java.util.stream.Collectors.toSet());

        if (!uploadedTypes.containsAll(REQUIRED_DOCUMENT_TYPES)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Company verification requires a business registration document"
            );
        }
    }

    private CompanyDocumentResponse toDocumentResponse(CompanyDocument document) {
        return new CompanyDocumentResponse(
                document.getId(),
                document.getCompany().getId(),
                document.getUploadedByRecruiterProfile().getId(),
                document.getDocumentType(),
                document.getDocumentUrl(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }

    private String normalizeDocumentType(String value) {
        String normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
