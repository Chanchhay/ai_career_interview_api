package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.features.company.dto.CompanyDocumentResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;

import java.util.List;

public record ModeratorCompanyDetailResponse(
        CompanyResponse company,
        List<CompanyDocumentResponse> documents,
        List<CompanyVerificationResponse> verificationHistory
) {
}
