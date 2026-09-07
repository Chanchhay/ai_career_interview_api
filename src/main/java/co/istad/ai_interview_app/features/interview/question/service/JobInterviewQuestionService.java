package co.istad.ai_interview_app.features.interview.question.service;

import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetResponse;
import java.util.UUID;

public interface JobInterviewQuestionService {

    JobInterviewQuestionSetResponse getSet(UUID jobId);

    JobInterviewQuestionSetResponse saveSet(UUID jobId, JobInterviewQuestionSetRequest request);
}
