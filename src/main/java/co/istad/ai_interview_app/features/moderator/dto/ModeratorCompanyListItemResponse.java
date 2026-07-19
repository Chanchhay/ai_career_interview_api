package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;

public record ModeratorCompanyListItemResponse(
        Long id,
        Long recruiterProfileId,
        Long industryId,
        String industryName,
        String name,
        String websiteUrl,
        String contactEmail,
        String businessRegistrationNo,
        VerificationStatus verificationStatus,
        ProfileStatus status
) {
}
