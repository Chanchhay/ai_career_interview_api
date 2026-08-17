package co.istad.ai_interview_app.features.recruiter.talent.service;

import co.istad.ai_interview_app.features.recruiter.talent.dto.ForwardedApplicationResponse;

import java.util.List;

public interface RecruiterForwardedApplicationService {

    List<ForwardedApplicationResponse> getForwardedApplications();

    ForwardedApplicationResponse getForwardedApplication(Long applicationId);
}
