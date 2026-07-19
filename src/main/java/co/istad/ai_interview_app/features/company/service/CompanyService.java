package co.istad.ai_interview_app.features.company.service;

import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyDocumentRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyDocumentResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyUpdateRequest;

import java.util.List;

public interface CompanyService {

    CompanyResponse createCompany(
            CompanyCreateRequest request
    );

    CompanyResponse getMyCompany();

    CompanyResponse updateCompany(
            Long id,
            CompanyUpdateRequest request
    );

    CompanyDocumentResponse addDocument(
            Long companyId,
            CompanyDocumentRequest request
    );

    List<CompanyDocumentResponse> getDocuments(Long companyId);

    void deleteDocument(Long companyId, Long documentId);

    CompanyResponse submitVerification(Long companyId);
}
