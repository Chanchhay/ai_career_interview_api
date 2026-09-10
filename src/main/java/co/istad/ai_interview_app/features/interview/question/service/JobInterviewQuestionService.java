package co.istad.ai_interview_app.features.interview.question.service;

import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetResponse;
import co.istad.ai_interview_app.features.interview.question.dto.PublicJobInterviewPreviewResponse;
import java.util.List;
import java.util.UUID;

public interface JobInterviewQuestionService {

    JobInterviewQuestionSetResponse getSet(UUID jobId);

    JobInterviewQuestionSetResponse saveSet(UUID jobId, JobInterviewQuestionSetRequest request);

    /**
     * The same questions with the rubric stripped, for a signed-out visitor
     * deciding whether to sit the interview.
     *
     * <p>Serves only published jobs, and throws 404 for anything else — an
     * unpublished job's questions are as private as the job itself.
     */
    PublicJobInterviewPreviewResponse getPublicPreview(UUID jobId);

    /**
     * Previews for a page of the job board, in the order the ids were given.
     *
     * <p>Ids that are not public are dropped rather than raising — a listing
     * built a moment ago can contain a job that has since expired, and losing
     * one preview is better than losing the whole section.
     */
    List<PublicJobInterviewPreviewResponse> getPublicPreviews(List<UUID> jobIds);
}
