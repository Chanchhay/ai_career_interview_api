package co.istad.ai_interview_app.features.interview.question.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewConfigService;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionResponse;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetResponse;
import co.istad.ai_interview_app.features.interview.question.entity.JobInterviewQuestion;
import co.istad.ai_interview_app.features.interview.question.repository.JobInterviewQuestionRepository;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Reading and rewriting a job's hand-written interview questions.
 *
 * <p>A save replaces the set: questions carrying an id are updated in place,
 * new ones are added, and anything the job holds that the request left out is
 * deleted. Updating in place rather than deleting and reinserting keeps the ids
 * stable across a save, so an editor that saves twice does not scatter the
 * screen's keys.
 */
@Service
@RequiredArgsConstructor
public class JobInterviewQuestionServiceImpl implements JobInterviewQuestionService {

    private final JobInterviewQuestionRepository questionRepository;
    private final JobPostRepository jobPostRepository;
    private final AiInterviewConfigService configService;

    @Override
    @Transactional(readOnly = true)
    public JobInterviewQuestionSetResponse getSet(Long jobId) {
        JobPost jobPost = requireJob(jobId);

        return toResponse(
                jobPost,
                questionRepository.findAllByJobPost_IdOrderByDisplayOrderAsc(jobId)
        );
    }

    @Override
    @Transactional
    public JobInterviewQuestionSetResponse saveSet(Long jobId, JobInterviewQuestionSetRequest request) {
        JobPost jobPost = requireJob(jobId);
        int defaultMaxScore = configService.currentGenerationConfig().maxScorePerQuestion();

        Map<Long, JobInterviewQuestion> existing = new LinkedHashMap<>();
        for (JobInterviewQuestion question : questionRepository.findAllByJobPost_IdOrderByDisplayOrderAsc(jobId)) {
            existing.put(question.getId(), question);
        }

        List<JobInterviewQuestion> saved = new ArrayList<>();
        int order = 1;

        for (JobInterviewQuestionRequest incoming : request.questions()) {
            JobInterviewQuestion question;

            if (incoming.id() == null) {
                question = new JobInterviewQuestion();
                question.setJobPost(jobPost);
            } else {
                question = existing.remove(incoming.id());

                /*
                 * An id that is not on this job means the editor is working from
                 * a stale copy, or is pointing at another job's question. Either
                 * way, silently creating a new row would hide the problem and
                 * leave the author looking at something they did not write.
                 */
                if (question == null) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Question " + incoming.id() + " is no longer on this job. Reload and try again."
                    );
                }
            }

            question.setQuestionText(incoming.questionText().trim());
            question.setQuestionType(incoming.questionType());
            question.setExpectedAnswer(normalizeBlankToNull(incoming.expectedAnswer()));
            question.setMaxScore(incoming.maxScore() == null ? defaultMaxScore : incoming.maxScore());
            question.setDisplayOrder(order++);

            saved.add(question);
        }

        // Whatever nothing claimed was removed on the screen.
        questionRepository.deleteAll(existing.values());
        questionRepository.saveAll(saved);

        jobPost.setManualQuestionMode(request.mode());
        jobPostRepository.save(jobPost);

        return toResponse(jobPost, saved);
    }

    private JobPost requireJob(Long jobId) {
        return jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job post was not found"));
    }

    private JobInterviewQuestionSetResponse toResponse(JobPost jobPost, List<JobInterviewQuestion> questions) {
        AiInterviewGenerationConfig config = configService.currentGenerationConfig();

        return new JobInterviewQuestionSetResponse(
                jobPost.getId(),
                jobPost.getTitle(),
                jobPost.getManualQuestionMode(),
                config.questionCount(),
                config.maxScorePerQuestion(),
                generatedCount(jobPost.getManualQuestionMode(), questions.size(), config.questionCount()),
                Arrays.asList(InterviewQuestionType.values()),
                questions.stream()
                        .map(question -> new JobInterviewQuestionResponse(
                                question.getId(),
                                question.getDisplayOrder(),
                                question.getQuestionType(),
                                question.getQuestionText(),
                                question.getExpectedAnswer(),
                                question.getMaxScore()
                        ))
                        .toList()
        );
    }

    /**
     * How many questions the AI would add.
     *
     * <p>Mirrors what session creation does, and is the one number an author
     * most wants to see before saving. A job with nothing written is still
     * generated in full — the mode only applies once there is something to
     * apply it to.
     */
    static int generatedCount(ManualQuestionMode mode, int writtenCount, int targetCount) {
        if (writtenCount == 0) return targetCount;
        if (mode == ManualQuestionMode.MANUAL_ONLY) return 0;

        return Math.max(0, targetCount - writtenCount);
    }
}
