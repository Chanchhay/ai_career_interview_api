package co.istad.ai_interview_app.features.recruiter.dto;

import jakarta.validation.constraints.Size;

public record RecruiterProfileUpdateRequest(
        @Size(max = 500, message = "Avatar URL must be at most 500 characters")
        String avatarUrl,

        @Size(max = 100, message = "Position must be at most 100 characters")
        String position,

        @Size(max = 255, message = "LinkedIn profile must be at most 255 characters")
        String linkedinUrl
) {
}
