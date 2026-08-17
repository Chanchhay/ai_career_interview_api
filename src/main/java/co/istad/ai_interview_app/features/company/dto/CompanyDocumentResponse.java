package co.istad.ai_interview_app.features.company.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;

import java.time.Instant;

public record CompanyDocumentResponse(
        Long id,
        Long companyId,
        Long uploadedByRecruiterProfileId,
        String documentType,
        String documentUrl,
        ProfileStatus status,
        Instant createdAt
) {
}
