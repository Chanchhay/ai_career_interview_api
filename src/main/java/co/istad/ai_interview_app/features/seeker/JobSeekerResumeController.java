package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.seeker.dto.ResumeCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.ResumeResponse;
import co.istad.ai_interview_app.features.seeker.dto.ResumeUpdateRequest;
import co.istad.ai_interview_app.features.seeker.service.JobSeekerResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-seeker/resumes")
@RequiredArgsConstructor
public class JobSeekerResumeController {

    private final JobSeekerResumeService resumeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<ResumeResponse> create(
            @Valid @RequestBody ResumeCreateRequest request
    ) {
        return ApiResponse.success(resumeService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<List<ResumeResponse>> getMyResumes() {
        return ApiResponse.success(resumeService.getMyResumes());
    }

    @GetMapping("/{resumeId}")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<ResumeResponse> getMyResume(@PathVariable Long resumeId) {
        return ApiResponse.success(resumeService.getMyResume(resumeId));
    }

    @PatchMapping("/{resumeId}")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<ResumeResponse> update(
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeUpdateRequest request
    ) {
        return ApiResponse.success(resumeService.update(resumeId, request));
    }

    @DeleteMapping("/{resumeId}")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<Void> delete(@PathVariable Long resumeId) {
        resumeService.delete(resumeId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{resumeId}/default")
    @PreAuthorize("hasAnyRole('SEEKER','JOB_SEEKER')")
    public ApiResponse<ResumeResponse> setDefault(@PathVariable Long resumeId) {
        return ApiResponse.success(resumeService.setDefault(resumeId));
    }
}
