package co.istad.ai_interview_app.features.interview.ai;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewService;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiCallBindingRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.VoiceTranscriptRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<AiInterviewSessionResponse> createInterviewForJob(
            @PathVariable Long jobId
    ) {
        return ApiResponse.success(aiInterviewService.createInterviewForJob(jobId));
    }

    @PostMapping("/applications/{applicationId}/ai-interviews")
    public ApiResponse<AiInterviewSessionResponse> createInterviewForApplication(
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(aiInterviewService.createInterviewForApplication(applicationId));
    }

    @GetMapping("/ai-interviews")
    public ApiResponse<List<AiInterviewSessionResponse>> getMyInterviews() {
        return ApiResponse.success(aiInterviewService.getMyInterviews());
    }

    @GetMapping("/ai-interviews/{sessionId}")
    public ApiResponse<AiInterviewSessionResponse> getMyInterview(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(aiInterviewService.getMyInterview(sessionId));
    }

    @PostMapping("/ai-interviews/{sessionId}/start")
    public ApiResponse<AiInterviewSessionResponse> startInterview(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(aiInterviewService.startInterview(sessionId));
    }

    @PutMapping("/ai-interviews/{sessionId}/questions/{questionId}/answer")
    public ApiResponse<AiInterviewSessionResponse> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody AiInterviewAnswerRequest request
    ) {
        return ApiResponse.success(aiInterviewService.submitAnswer(sessionId, questionId, request));
    }

    /** Attaches the Vapi call that is voicing this interview, so its webhook can find the session. */
    @PutMapping("/ai-interviews/{sessionId}/vapi-call")
    public ApiResponse<AiInterviewSessionResponse> bindVapiCall(
            @PathVariable Long sessionId,
            @Valid @RequestBody VapiCallBindingRequest request
    ) {
        return ApiResponse.success(aiInterviewService.bindVapiCall(sessionId, request));
    }

    /**
     * Submits the transcript of a finished voice interview for scoring.
     *
     * <p>Runs the same work as Vapi's end-of-call webhook. Both exist because the
     * webhook needs a publicly reachable server, which local development lacks,
     * and the browser cannot report a call abandoned by closing the tab.
     */
    @PostMapping("/ai-interviews/{sessionId}/transcript")
    public ApiResponse<AiInterviewSessionResponse> submitVoiceTranscript(
            @PathVariable Long sessionId,
            @Valid @RequestBody VoiceTranscriptRequest request
    ) {
        return ApiResponse.success(aiInterviewService.submitVoiceTranscript(sessionId, request));
    }

    @PostMapping("/ai-interviews/{sessionId}/complete")
    public ApiResponse<AiInterviewResultResponse> completeInterview(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(aiInterviewService.completeInterview(sessionId));
    }

    @GetMapping("/ai-interviews/{sessionId}/result")
    public ApiResponse<AiInterviewResultResponse> getResult(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(aiInterviewService.getResult(sessionId));
    }
}
