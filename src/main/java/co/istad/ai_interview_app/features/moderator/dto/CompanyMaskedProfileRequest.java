package co.istad.ai_interview_app.features.moderator.dto;

import jakarta.validation.constraints.Size;

/**
 * Sets the stand-in logo candidates see while a company is masked.
 *
 * <p>Nullable on purpose, and clearing is a first-class outcome rather than a
 * separate endpoint: sending null (or blank) removes the stand-in and returns
 * the company to showing no mark at all. An administrator who put up the wrong
 * image needs to take it down at least as urgently as they put it up.
 */
public record CompanyMaskedProfileRequest(

        @Size(max = 500, message = "Logo URL must be at most 500 characters")
        String maskedLogoUrl
) {
}
