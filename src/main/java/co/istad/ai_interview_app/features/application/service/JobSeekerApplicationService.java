package co.istad.ai_interview_app.features.application.service;

import co.istad.ai_interview_app.features.application.dto.JobApplicationCreateRequest;
import co.istad.ai_interview_app.features.application.dto.JobApplicationResponse;

import java.util.List;

public interface JobSeekerApplicationService {

    JobApplicationResponse apply(Long jobId, JobApplicationCreateRequest request);

    List<JobApplicationResponse> getMyApplications();

    JobApplicationResponse getMyApplication(Long applicationId);

    JobApplicationResponse withdraw(Long applicationId);
}
