package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;
import jakarta.validation.constraints.NotNull;

/**
 * Masks or unmasks a company for candidates.
 *
 * <p>The whole value is one enum rather than a boolean so the two states are
 * named in the request and in the log. "visibility=MASKED" says what happened;
 * "masked=true" needs the field name to be read carefully to mean anything.
 */
public record CompanyIdentityVisibilityRequest(

        @NotNull(message = "Choose whether candidates see this company")
        CompanyIdentityVisibility visibility
) {
}
