package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.ResumeCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.ResumeResponse;
import co.istad.ai_interview_app.features.seeker.dto.ResumeUpdateRequest;

import co.istad.ai_interview_app.features.file.dto.DownloadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface JobSeekerResumeService {

    ResumeResponse create(ResumeCreateRequest request);

    List<ResumeResponse> getMyResumes();

    ResumeResponse getMyResume(Long resumeId);

    ResumeResponse update(Long resumeId, ResumeUpdateRequest request);

    void delete(Long resumeId);

    ResumeResponse setDefault(Long resumeId);

    /** Renders the resume's structured data to a PDF and stores it. */
    ResumeResponse generate(Long resumeId);

    /** Creates a resume from a PDF or DOCX the job seeker supplies. */
    ResumeResponse uploadOwnResume(String title, MultipartFile file);

    /** The stored file's bytes plus a filename, for the download endpoint. */
    DownloadedFile download(Long resumeId);
}
