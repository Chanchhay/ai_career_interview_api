package co.istad.ai_interview_app.features.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
        Long industryId,

        @NotBlank(message = "Company name is required")
        @Size(max = 200, message = "Company name must be at most 200 characters")
        String name,

        String description,

        @Size(max = 255, message = "Website URL must be at most 255 characters")
        String websiteUrl,

        String address,

        @Email(message = "Contact email must be valid")
        @Size(max = 255, message = "Contact email must be at most 255 characters")
        String contactEmail,

        @Size(max = 255, message = "Contact phone must be at most 255 characters")
        String contactPhone,

        @Size(max = 255, message = "Logo URL must be at most 255 characters")
        String logoUrl,

        @Size(max = 100, message = "Business registration number must be at most 100 characters")
        String businessRegistrationNo
) {
}
