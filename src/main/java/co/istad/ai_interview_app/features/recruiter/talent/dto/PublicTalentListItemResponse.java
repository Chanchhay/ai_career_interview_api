package co.istad.ai_interview_app.features.recruiter.talent.dto;

import co.istad.ai_interview_app.shared.enums.profile.SalaryVisibility;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicTalentListItemResponse(
        UUID profileId,
        String publicProfileSlug,
        String avatarUrl,
        String headline,
        String bio,
        String currentPosition,
        String preferredLocation,
        String availabilityStatus,
        BigDecimal expectedSalaryMin,
        BigDecimal expectedSalaryMax,
        String expectedSalaryCurrency,
        SalaryVisibility salaryVisibility
) {
}
