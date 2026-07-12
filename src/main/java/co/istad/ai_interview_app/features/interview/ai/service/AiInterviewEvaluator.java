package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.InterviewEvaluationRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.InterviewEvaluationResult;

public interface AiInterviewEvaluator {

    InterviewEvaluationResult evaluate(InterviewEvaluationRequest request);
}
