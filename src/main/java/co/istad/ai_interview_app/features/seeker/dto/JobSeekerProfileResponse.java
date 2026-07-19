package co.istad.ai_interview_app.features.seeker.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.profile.SalaryVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record JobSeekerProfileResponse(
        Long id,
        String headline,
        String bio,
        String currentPosition,
        BigDecimal expectedSalaryMin,
        BigDecimal expectedSalaryMax,
        String expectedSalaryCurrency,
        SalaryVisibility salaryVisibility,
        String preferredLocation,
        String availabilityStatus,
        String publicProfileSlug,
        VisibilityStatus profileVisibility,
        Instant publishedAt,
        VerificationStatus verificationStatus,
        ProfileStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
