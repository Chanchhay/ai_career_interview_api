package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import java.util.UUID;

public record ModeratorCompanyListItemResponse(
        UUID id,
        UUID recruiterProfileId,
        UUID industryId,
        String industryName,
        String name,
        /** The company's own logo, so the queue can show who it is looking at. */
        String logoUrl,
        String websiteUrl,
        String contactEmail,
        String businessRegistrationNo,
        VerificationStatus verificationStatus,
        ProfileStatus status,
        CompanyIdentityVisibility identityVisibility,
        /** How much this company has posted, and how much of it is live. */
        long jobCount,
        long publishedJobCount
) {
}
