package co.istad.ai_interview_app.features.recruiter.dto;

import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import java.util.UUID;

public record RecruiterProfileResponse(
        UUID id,
        String avatarUrl,
        String position,
        String linkedinUrl,
        ProfileStatus status
) {
}
