package co.istad.ai_interview_app.features.company.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record CompanyDocumentResponse(
        UUID id,
        UUID companyId,
        UUID uploadedByRecruiterProfileId,
        String documentType,
        String documentUrl,
        ProfileStatus status,
        Instant createdAt
) {
}
