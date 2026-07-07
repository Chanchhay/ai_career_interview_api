package co.istad.ai_interview_app.features.company.service;

import co.istad.ai_interview_app.features.company.dto.CompanyCreateRequest;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;

public interface CompanyService {

    CompanyResponse createCompany(
            CompanyCreateRequest request
    );
}
