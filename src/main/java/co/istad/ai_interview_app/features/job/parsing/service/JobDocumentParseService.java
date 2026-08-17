package co.istad.ai_interview_app.features.job.parsing.service;

import co.istad.ai_interview_app.features.job.parsing.dto.JobDocumentParseResponse;
import org.springframework.web.multipart.MultipartFile;

public interface JobDocumentParseService {

    /**
     * Reads an uploaded PDF job description and returns the job post fields it
     * describes, for a recruiter to review before saving.
     *
     * <p>Stores the PDF privately as a side effect; nothing else is persisted.
     */
    JobDocumentParseResponse parse(MultipartFile file);
}
