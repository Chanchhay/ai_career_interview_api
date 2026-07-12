package co.istad.ai_interview_app.features.interview.ai;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-seeker")
@RequiredArgsConstructor
public class AiInterviewController {

    private final AiInterviewService aiInterviewService;

    @PostMapping("/jobs/{jobId}/ai-interviews")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<AiInterviewSessionResponse> createInterviewForJob(
            @PathVariable Long jobId
    ) {
        return ApiResponse.success(aiInterviewService.createInterviewForJob(jobId));
    }

    @GetMapping("/ai-interviews")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<List<AiInterviewSessionResponse>> getMyInterviews() {
        return ApiResponse.success(aiInterviewService.getMyInterviews());
    }

    @GetMapping("/ai-interviews/{sessionId}")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<AiInterviewSessionResponse> getMyInterview(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(aiInterviewService.getMyInterview(sessionId));
    }

    @PostMapping("/ai-interviews/{sessionId}/start")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<AiInterviewSessionResponse> startInterview(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(aiInterviewService.startInterview(sessionId));
    }

    @PutMapping("/ai-interviews/{sessionId}/questions/{questionId}/answer")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<AiInterviewSessionResponse> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody AiInterviewAnswerRequest request
    ) {
        return ApiResponse.success(aiInterviewService.submitAnswer(sessionId, questionId, request));
    }

    @PostMapping("/ai-interviews/{sessionId}/complete")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<AiInterviewResultResponse> completeInterview(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(aiInterviewService.completeInterview(sessionId));
    }

    @GetMapping("/ai-interviews/{sessionId}/result")
    @PreAuthorize("hasRole('SEEKER')")
    public ApiResponse<AiInterviewResultResponse> getResult(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(aiInterviewService.getResult(sessionId));
    }
}
