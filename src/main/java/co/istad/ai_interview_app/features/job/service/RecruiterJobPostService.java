package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.job.dto.JobPostRequest;
import co.istad.ai_interview_app.features.job.dto.JobPostResponse;

import java.util.List;

public interface RecruiterJobPostService {

    JobPostResponse createJobDraft(JobPostRequest request);

    List<JobPostResponse> getMyJobs();

    JobPostResponse getMyJob(Long id);

    JobPostResponse updateMyJob(Long id, JobPostRequest request);

    JobPostResponse publishMyJob(Long id);

    JobPostResponse pauseMyJob(Long id);

    JobPostResponse closeMyJob(Long id);
}
