package co.istad.ai_interview_app.features.company.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        UUID recruiterProfileId,
        UUID industryId,
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
        CompanyIdentityVisibility identityVisibility,
        /**
         * The stand-in logo candidates see while this company is masked. Only
         * an administrator can set it; it is readable here so the moderation
         * console can show what is currently in place.
         */
        String maskedLogoUrl
) {
}
