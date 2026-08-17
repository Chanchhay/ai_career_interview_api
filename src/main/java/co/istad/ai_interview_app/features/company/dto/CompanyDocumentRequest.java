package co.istad.ai_interview_app.features.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyDocumentRequest(
        @NotBlank(message = "Document type is required")
        @Size(max = 100, message = "Document type must be at most 100 characters")
        String documentType,

        @NotBlank(message = "Document URL is required")
        @Size(max = 500, message = "Document URL must be at most 500 characters")
        String documentUrl
) {
}
