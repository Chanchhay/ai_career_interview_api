package co.istad.ai_interview_app.features.recruiter.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;

public record RecruiterProfileResponse(
        Long id,
        String position,
        String linkedinUrl,
        ProfileStatus status
) {
}
