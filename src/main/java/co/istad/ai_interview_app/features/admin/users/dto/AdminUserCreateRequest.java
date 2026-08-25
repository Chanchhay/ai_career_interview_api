package co.istad.ai_interview_app.features.admin.users.dto;

import co.istad.ai_interview_app.shared.enums.account.ManageableRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Creates a staff account directly, which self-registration cannot do:
 * {@code /auth/register} only issues SEEKER and RECRUITER.
 */
public record AdminUserCreateRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @NotBlank @Size(min = 8, max = 100) String password,
        /**
         * When true the user must change the password at first login. Left to
         * the caller rather than forced, because a break-glass account created
         * during an incident should not depend on a password-reset flow.
         */
        Boolean temporaryPassword,
        @NotEmpty List<ManageableRole> roles
) {
}
