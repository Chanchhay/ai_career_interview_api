package co.istad.ai_interview_app.features.recruiter.talent.dto;

import co.istad.ai_interview_app.shared.enums.profile.SalaryVisibility;

import java.math.BigDecimal;

public record PublicTalentListItemResponse(
        Long profileId,
        String publicProfileSlug,
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
