package co.istad.ai_interview_app.features.company.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;

public record CompanyResponse(
        Long id,
        Long recruiterProfileId,
        Long industryId,
        String industryName,
        String name,
        String description,
        String websiteUrl,
        String address,
        String contactEmail,
        String contactPhone,
        String logoUrl,
        String businessRegistrationNo,
        VerificationStatus verificationStatus,
        ProfileStatus status,
        /**
         * Whether candidates are told who this company is. Readable by the
         * recruiter — being masked is not a secret from the company itself —
         * but only an administrator can change it.
         */
        CompanyIdentityVisibility identityVisibility
) {
}
