package co.istad.ai_interview_app.features.job.parsing;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.job.parsing.dto.JobDocumentParseResponse;
import co.istad.ai_interview_app.features.job.parsing.service.JobDocumentParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lets a recruiter start a job post from the PDF job description they already
 * have, instead of retyping it into the form.
 *
 * <p>Parsing never creates a job post: the response is prefill data the
 * recruiter reviews and then saves through
 * {@code /api/v1/recruiter/jobs} as usual.
 */
@RestController
@RequestMapping("/api/v1/recruiter/jobs")
@RequiredArgsConstructor
public class JobDocumentParseController {

    private final JobDocumentParseService jobDocumentParseService;

    @PostMapping(
            path = "/parse-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<JobDocumentParseResponse> parseDocument(
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(jobDocumentParseService.parse(file));
    }
}
