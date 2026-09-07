package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.job.dto.JobPostRequest;
import co.istad.ai_interview_app.features.job.dto.JobPostResponse;

import java.util.List;
import java.util.UUID;

public interface RecruiterJobPostService {

    JobPostResponse createJobDraft(JobPostRequest request);

    List<JobPostResponse> getMyJobs();

    JobPostResponse getMyJob(UUID id);

    JobPostResponse updateMyJob(UUID id, JobPostRequest request);

    JobPostResponse publishMyJob(UUID id);

    JobPostResponse pauseMyJob(UUID id);

    JobPostResponse resumeMyJob(UUID id);

    JobPostResponse closeMyJob(UUID id);
}
