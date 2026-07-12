package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;

import java.util.List;

public interface AiInterviewService {

    AiInterviewSessionResponse createInterviewForJob(Long jobId);

    List<AiInterviewSessionResponse> getMyInterviews();

    AiInterviewSessionResponse getMyInterview(Long sessionId);

    AiInterviewSessionResponse startInterview(Long sessionId);

    AiInterviewSessionResponse submitAnswer(Long sessionId, Long questionId, AiInterviewAnswerRequest request);

    AiInterviewResultResponse completeInterview(Long sessionId);

    AiInterviewResultResponse getResult(Long sessionId);
}
