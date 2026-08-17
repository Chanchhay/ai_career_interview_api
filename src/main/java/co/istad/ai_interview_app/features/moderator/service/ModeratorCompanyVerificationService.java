package co.istad.ai_interview_app.features.moderator.service;

import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyListItemResponse;
import co.istad.ai_interview_app.features.moderator.dto.CompanyVerificationResponse;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ModeratorCompanyVerificationService {

    Page<ModeratorCompanyListItemResponse> getCompanies(
            VerificationStatus verificationStatus,
            Pageable pageable
    );

    ModeratorCompanyDetailResponse getCompany(Long companyId);

    CompanyVerificationResponse approve(Long companyId, DecisionRequest request);

    CompanyVerificationResponse reject(Long companyId, DecisionRequest request);

    CompanyVerificationResponse requestRevision(Long companyId, DecisionRequest request);
}
