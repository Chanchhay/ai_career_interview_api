package co.istad.ai_interview_app.features.interview.question.service;

import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetResponse;

public interface JobInterviewQuestionService {

    JobInterviewQuestionSetResponse getSet(Long jobId);

    JobInterviewQuestionSetResponse saveSet(Long jobId, JobInterviewQuestionSetRequest request);
}
