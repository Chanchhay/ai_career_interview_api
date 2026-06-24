package co.istad.ai_interview_app.features.recruiter.service;

import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileResponse;
import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileUpdateRequest;

public interface RecruiterProfileService {

    RecruiterProfileResponse updateMyProfile(
            String keycloakUserId,
            RecruiterProfileUpdateRequest request
    );
}
