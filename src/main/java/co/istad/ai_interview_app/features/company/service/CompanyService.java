package co.istad.ai_interview_app.features.company.service;

import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyDocumentRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyDocumentResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface CompanyService {

    CompanyResponse createCompany(
            CompanyCreateRequest request
    );

    CompanyResponse getMyCompany();

    CompanyResponse updateCompany(
            UUID id,
            CompanyUpdateRequest request
    );

    CompanyDocumentResponse addDocument(
            UUID companyId,
            CompanyDocumentRequest request
    );

    List<CompanyDocumentResponse> getDocuments(UUID companyId);

    void deleteDocument(UUID companyId, UUID documentId);

    CompanyResponse submitVerification(UUID companyId);
}
