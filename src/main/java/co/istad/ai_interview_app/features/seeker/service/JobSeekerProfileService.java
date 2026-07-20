package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.JobSeekerProfileResponse;
import co.istad.ai_interview_app.features.seeker.dto.JobSeekerProfileUpdateRequest;

public interface JobSeekerProfileService {

    JobSeekerProfileResponse getMyProfile();

    JobSeekerProfileResponse updateMyProfile(JobSeekerProfileUpdateRequest request);
}
