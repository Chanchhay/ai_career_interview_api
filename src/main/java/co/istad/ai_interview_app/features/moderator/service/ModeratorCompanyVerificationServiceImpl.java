package co.istad.ai_interview_app.features.moderator.service;

import co.istad.ai_interview_app.features.company.dto.CompanyDocumentResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.entity.CompanyDocument;
import co.istad.ai_interview_app.features.company.mapper.CompanyMapper;
import co.istad.ai_interview_app.features.company.repository.CompanyDocumentRepository;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.moderator.dto.CompanyVerificationResponse;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyListItemResponse;
import co.istad.ai_interview_app.features.moderator.entity.CompanyVerification;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.moderator.repository.CompanyVerificationRepository;
import co.istad.ai_interview_app.shared.enums.moderation.ModerationDecision;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class ModeratorCompanyVerificationServiceImpl implements ModeratorCompanyVerificationService {

    private final CompanyRepository companyRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final CompanyVerificationRepository companyVerificationRepository;
    private final AuthenticatedModeratorProfileResolver moderatorProfileResolver;
    private final CompanyMapper companyMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ModeratorCompanyListItemResponse> getCompanies(
            VerificationStatus verificationStatus,
            Pageable pageable
    ) {
        Page<Company> companies = verificationStatus == null
                ? companyRepository.findAll(pageable)
                : companyRepository.findAllByVerificationStatus(verificationStatus, pageable);

        return companies.map(this::toListItemResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ModeratorCompanyDetailResponse getCompany(Long companyId) {
        Company company = resolveCompany(companyId);
        CompanyResponse companyResponse = companyMapper.toResponse(company);
        List<CompanyDocumentResponse> documents = companyDocumentRepository
                .findAllByCompany_IdOrderByCreatedAtDesc(company.getId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        List<CompanyVerificationResponse> verificationHistory = companyVerificationRepository
                .findAllByCompany_IdOrderByVerifiedAtDesc(company.getId())
                .stream()
                .map(this::toVerificationResponse)
                .toList();

        return new ModeratorCompanyDetailResponse(companyResponse, documents, verificationHistory);
    }

    @Override
    @Transactional
    public CompanyVerificationResponse approve(Long companyId, DecisionRequest request) {
        Company company = resolveCompany(companyId);
        CompanyVerification verification = recordDecision(
                company,
                ModerationDecision.APPROVED,
                normalizeBlankToNull(request.decisionNote())
        );

        company.setVerificationStatus(VerificationStatus.APPROVED);
        company.setStatus(ProfileStatus.ACTIVE);

        return toVerificationResponse(verification);
    }

    @Override
    @Transactional
    public CompanyVerificationResponse reject(Long companyId, DecisionRequest request) {
        String note = requireDecisionNote(request);
        Company company = resolveCompany(companyId);
        CompanyVerification verification = recordDecision(company, ModerationDecision.REJECTED, note);

        company.setVerificationStatus(VerificationStatus.REJECTED);
        company.setStatus(ProfileStatus.PENDING);

        return toVerificationResponse(verification);
    }

    @Override
    @Transactional
    public CompanyVerificationResponse requestRevision(Long companyId, DecisionRequest request) {
        String note = requireDecisionNote(request);
        Company company = resolveCompany(companyId);
        CompanyVerification verification = recordDecision(company, ModerationDecision.NEEDS_REVISION, note);

        company.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        company.setStatus(ProfileStatus.PENDING);

        return toVerificationResponse(verification);
    }

    private Company resolveCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company was not found"));
    }

    private CompanyVerification recordDecision(
            Company company,
            ModerationDecision decision,
            String note
    ) {
        ModeratorProfile moderator = moderatorProfileResolver.resolve();

        CompanyVerification verification = new CompanyVerification();
        verification.setCompany(company);
        verification.setModeratorProfile(moderator);
        verification.setDecision(decision);
        verification.setNote(note);
        verification.setVerifiedAt(Instant.now());

        return companyVerificationRepository.save(verification);
    }

    private String requireDecisionNote(DecisionRequest request) {
        String note = normalizeBlankToNull(request.decisionNote());
        if (!hasText(note)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision note is required");
        }
        return note;
    }

    private ModeratorCompanyListItemResponse toListItemResponse(Company company) {
        return new ModeratorCompanyListItemResponse(
                company.getId(),
                company.getRecruiterProfile().getId(),
                company.getIndustry() == null ? null : company.getIndustry().getId(),
                company.getIndustry() == null ? null : company.getIndustry().getName(),
                company.getName(),
                company.getWebsiteUrl(),
                company.getContactEmail(),
                company.getBusinessRegistrationNo(),
                company.getVerificationStatus(),
                company.getStatus()
        );
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

    private CompanyVerificationResponse toVerificationResponse(CompanyVerification verification) {
        return new CompanyVerificationResponse(
                verification.getId(),
                verification.getCompany().getId(),
                verification.getModeratorProfile().getId(),
                verification.getDecision(),
                verification.getNote(),
                verification.getVerifiedAt()
        );
    }
}
