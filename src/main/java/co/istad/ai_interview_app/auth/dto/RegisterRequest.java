package co.istad.ai_interview_app.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username cannot be empty")
        @Size(min = 3, max = 255)
        String username,

        @NotBlank(message = "password cannot be empty")
        @Size(min = 8, max = 255)
        String password,

        @NotBlank(message = "confirm password cannot be empty")
        @Size(min = 8, max = 255)
        String confirmPassword,

        @NotBlank(message = "email cannot be empty")
        @Email
        String email,

        @NotBlank(message = "firstname cannot be empty")
        String firstName,

        @NotBlank(message = "lastname cannot be empty")
        String lastName,

        GenderOptions gender,

        RegistrationRole role,

        @JsonAlias("phone_number")
        @Size(min = 8, max = 30)
        @Pattern(
                regexp = "^\\+?[0-9 ]+$",
                message = "Phone number may contain digits, spaces, and an optional leading plus sign."
        )
        String phoneNumber
) {
}
