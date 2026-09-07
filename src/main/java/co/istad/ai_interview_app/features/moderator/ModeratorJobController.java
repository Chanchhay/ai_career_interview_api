package co.istad.ai_interview_app.features.moderator;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorJobListItemResponse;
import co.istad.ai_interview_app.features.job.dto.JobPostResponse;
import co.istad.ai_interview_app.features.moderator.service.ModeratorCompanyVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Moderator actions on an individual job post.
 *
 * <p>Drafting, editing and publishing stay recruiter-owned — this is only the
 * power to take a live posting down and to put it back. Each action is written
 * to the job's moderation log, so a recruiter whose post disappears can be told
 * who did it and why.
 */
@RestController
@RequestMapping("/api/v1/moderator/jobs")
@RequiredArgsConstructor
public class ModeratorJobController {

    private final ModeratorCompanyVerificationService companyVerificationService;

    /** One job in full, in any state. */
    @GetMapping("/{jobId}")
    public ApiResponse<JobPostResponse> getJob(@PathVariable UUID jobId) {
        return ApiResponse.success(companyVerificationService.getJob(jobId));
    }

    /** Hides a published job from candidates. Requires a note. */
    @PostMapping("/{jobId}/pause")
    public ApiResponse<ModeratorJobListItemResponse> pause(
            @PathVariable UUID jobId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.pauseJob(jobId, request));
    }

    /** Puts a paused job back in front of candidates. */
    @PostMapping("/{jobId}/resume")
    public ApiResponse<ModeratorJobListItemResponse> resume(
            @PathVariable UUID jobId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.resumeJob(jobId, request));
    }

    /** Closes a job for good. Requires a note. */
    @PostMapping("/{jobId}/close")
    public ApiResponse<ModeratorJobListItemResponse> close(
            @PathVariable UUID jobId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(companyVerificationService.closeJob(jobId, request));
    }
}
