package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.file.dto.DownloadedFile;
import co.istad.ai_interview_app.features.seeker.dto.ResumeCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.ResumeResponse;
import co.istad.ai_interview_app.features.seeker.dto.ResumeUpdateRequest;
import co.istad.ai_interview_app.features.seeker.service.JobSeekerResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-seeker/resumes")
@RequiredArgsConstructor
public class JobSeekerResumeController {

    private final JobSeekerResumeService resumeService;

    @PostMapping
    public ApiResponse<ResumeResponse> create(
            @Valid @RequestBody ResumeCreateRequest request
    ) {
        return ApiResponse.success(resumeService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ResumeResponse>> getMyResumes() {
        return ApiResponse.success(resumeService.getMyResumes());
    }

    @GetMapping("/{resumeId}")
    public ApiResponse<ResumeResponse> getMyResume(@PathVariable Long resumeId) {
        return ApiResponse.success(resumeService.getMyResume(resumeId));
    }

    @PatchMapping("/{resumeId}")
    public ApiResponse<ResumeResponse> update(
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeUpdateRequest request
    ) {
        return ApiResponse.success(resumeService.update(resumeId, request));
    }

    @DeleteMapping("/{resumeId}")
    public ApiResponse<Void> delete(@PathVariable Long resumeId) {
        resumeService.delete(resumeId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{resumeId}/default")
    public ApiResponse<ResumeResponse> setDefault(@PathVariable Long resumeId) {
        return ApiResponse.success(resumeService.setDefault(resumeId));
    }

    /**
     * Renders the resume's structured data into a stored PDF.
     *
     * <p>Explicit rather than automatic on save: generating is not free, and a
     * candidate editing their resume should not produce a new document on every
     * keystroke.
     */
    @PostMapping("/{resumeId}/generate")
    public ApiResponse<ResumeResponse> generate(
            @PathVariable Long resumeId
    ) {
        return ApiResponse.success(resumeService.generate(resumeId));
    }

    /** Creates a resume from a file the job seeker already has. Never parsed. */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResumeResponse> upload(
            @RequestParam String title,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(resumeService.uploadOwnResume(title, file));
    }

    /**
     * Streams the stored file back to its owner.
     *
     * <p>Returns bytes rather than the usual {@code ApiResponse} envelope, and
     * streams rather than redirecting to a presigned URL — see the service for
     * why that choice is deliberate here.
     */
    @GetMapping("/{resumeId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long resumeId
    ) {
        DownloadedFile file = resumeService.download(resumeId);

        if (file.isRedirect()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, file.redirectUrl())
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString()
                )
                .body(file.content());
    }
}
