package co.istad.ai_interview_app.features.seeker.dto;

import co.istad.ai_interview_app.shared.enums.profile.SalaryVisibility;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record JobSeekerProfileUpdateRequest(
        @Size(max = 255, message = "Headline must be at most 255 characters")
        String headline,
        @Size(max = 5000, message = "Bio must be at most 5000 characters")
        String bio,
        @Size(max = 150, message = "Current position must be at most 150 characters")
        String currentPosition,
        BigDecimal expectedSalaryMin,
        BigDecimal expectedSalaryMax,
        @Size(max = 10, message = "Expected salary currency must be at most 10 characters")
        String expectedSalaryCurrency,
        SalaryVisibility salaryVisibility,
        @Size(max = 150, message = "Preferred location must be at most 150 characters")
        String preferredLocation,
        @Size(max = 50, message = "Availability status must be at most 50 characters")
        String availabilityStatus
) {
}
