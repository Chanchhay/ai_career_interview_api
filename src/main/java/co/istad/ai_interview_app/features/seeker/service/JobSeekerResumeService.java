package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.ResumeCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.ResumeResponse;
import co.istad.ai_interview_app.features.seeker.dto.ResumeUpdateRequest;

import java.util.List;

public interface JobSeekerResumeService {

    ResumeResponse create(ResumeCreateRequest request);

    List<ResumeResponse> getMyResumes();

    ResumeResponse getMyResume(Long resumeId);

    ResumeResponse update(Long resumeId, ResumeUpdateRequest request);

    void delete(Long resumeId);

    ResumeResponse setDefault(Long resumeId);
}
