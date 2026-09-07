package co.istad.ai_interview_app.features.recruiter.talent.service;

import co.istad.ai_interview_app.features.recruiter.talent.dto.ForwardedApplicationResponse;

import java.util.List;
import java.util.UUID;

public interface RecruiterForwardedApplicationService {

    List<ForwardedApplicationResponse> getForwardedApplications();

    ForwardedApplicationResponse getForwardedApplication(UUID applicationId);
}
