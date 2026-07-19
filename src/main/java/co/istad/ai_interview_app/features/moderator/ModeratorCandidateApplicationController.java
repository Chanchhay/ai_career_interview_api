package co.istad.ai_interview_app.features.moderator;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationListItemResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationReviewResponse;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewCompleteRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewRequest;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewResponse;
import co.istad.ai_interview_app.features.moderator.service.ModeratorCandidateApplicationService;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/moderator")
@RequiredArgsConstructor
public class ModeratorCandidateApplicationController {

    private final ModeratorCandidateApplicationService applicationService;

    @GetMapping("/candidate-applications")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<Page<CandidateApplicationListItemResponse>> getReviewQueue(
            @RequestParam(required = false) CandidateApplicationReviewStatus status,
            Pageable pageable
    ) {
        return ApiResponse.success(applicationService.getReviewQueue(status, pageable));
    }

    @GetMapping("/candidate-applications/{applicationId}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<CandidateApplicationDetailResponse> getReviewDetail(
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(applicationService.getReviewDetail(applicationId));
    }

    @PostMapping("/candidate-applications/{applicationId}/human-interviews")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<HumanInterviewResponse> scheduleHumanInterview(
            @PathVariable Long applicationId,
            @Valid @RequestBody HumanInterviewRequest request
    ) {
        return ApiResponse.success(applicationService.scheduleHumanInterview(applicationId, request));
    }

    @PatchMapping("/human-interviews/{interviewId}/reschedule")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<HumanInterviewResponse> rescheduleHumanInterview(
            @PathVariable Long interviewId,
            @Valid @RequestBody HumanInterviewRequest request
    ) {
        return ApiResponse.success(applicationService.rescheduleHumanInterview(interviewId, request));
    }

    @PostMapping("/human-interviews/{interviewId}/complete")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<HumanInterviewResponse> completeHumanInterview(
            @PathVariable Long interviewId,
            @Valid @RequestBody HumanInterviewCompleteRequest request
    ) {
        return ApiResponse.success(applicationService.completeHumanInterview(interviewId, request));
    }

    @PostMapping("/human-interviews/{interviewId}/cancel")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<HumanInterviewResponse> cancelHumanInterview(
            @PathVariable Long interviewId
    ) {
        return ApiResponse.success(applicationService.cancelHumanInterview(interviewId));
    }

    @PostMapping("/candidate-applications/{applicationId}/approve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<CandidateApplicationReviewResponse> approve(
            @PathVariable Long applicationId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(applicationService.approve(applicationId, request));
    }

    @PostMapping("/candidate-applications/{applicationId}/reject")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<CandidateApplicationReviewResponse> reject(
            @PathVariable Long applicationId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.success(applicationService.reject(applicationId, request));
    }

    @PostMapping("/candidate-applications/{applicationId}/forward")
    @PreAuthorize("hasRole('MODERATOR')")
    public ApiResponse<CandidateApplicationReviewResponse> forward(
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(applicationService.forward(applicationId));
    }
}
