package co.istad.ai_interview_app.features.application.service;

import co.istad.ai_interview_app.features.application.dto.JobApplicationCreateRequest;
import co.istad.ai_interview_app.features.application.dto.JobApplicationResponse;

import java.util.List;
import java.util.UUID;

public interface JobSeekerApplicationService {

    JobApplicationResponse apply(UUID jobId, JobApplicationCreateRequest request);

    List<JobApplicationResponse> getMyApplications();

    JobApplicationResponse getMyApplication(UUID applicationId);

    JobApplicationResponse withdraw(UUID applicationId);
}
