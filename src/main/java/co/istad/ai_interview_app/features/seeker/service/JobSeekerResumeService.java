package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.ResumeCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.ResumeResponse;
import co.istad.ai_interview_app.features.seeker.dto.ResumeUpdateRequest;

import co.istad.ai_interview_app.features.file.dto.DownloadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface JobSeekerResumeService {

    ResumeResponse create(ResumeCreateRequest request);

    List<ResumeResponse> getMyResumes();

    ResumeResponse getMyResume(UUID resumeId);

    ResumeResponse update(UUID resumeId, ResumeUpdateRequest request);

    void delete(UUID resumeId);

    ResumeResponse setDefault(UUID resumeId);

    /** Renders the resume's structured data to a PDF and stores it. */
    ResumeResponse generate(UUID resumeId);

    /** Creates a resume from a PDF or DOCX the job seeker supplies. */
    ResumeResponse uploadOwnResume(String title, MultipartFile file);

    /** The stored file's bytes plus a filename, for the download endpoint. */
    DownloadedFile download(UUID resumeId);
}
