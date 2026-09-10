package co.istad.ai_interview_app.features.interview.question.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewConfigService;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionResponse;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetResponse;
import co.istad.ai_interview_app.features.interview.question.dto.PublicJobInterviewPreviewResponse;
import co.istad.ai_interview_app.features.interview.question.dto.PublicJobInterviewQuestionResponse;
import co.istad.ai_interview_app.features.interview.question.entity.JobInterviewQuestion;
import co.istad.ai_interview_app.features.interview.question.repository.JobInterviewQuestionRepository;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;
import java.util.UUID;

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

    /**
     * Minutes a candidate spends on one question, used only to put a rough
     * number on the practice page. Two minutes is the AI interview's own pacing
     * — long enough to answer, short enough that nobody treats it as a promise.
     */
    private static final int MINUTES_PER_QUESTION = 2;

    private final JobInterviewQuestionRepository questionRepository;
    private final JobPostRepository jobPostRepository;
    private final AiInterviewConfigService configService;

    @Override
    @Transactional(readOnly = true)
    public JobInterviewQuestionSetResponse getSet(UUID jobId) {
        JobPost jobPost = requireJob(jobId);

        return toResponse(
                jobPost,
                questionRepository.findAllByJobPost_IdOrderByDisplayOrderAsc(jobId)
        );
    }

    @Override
    @Transactional
    public JobInterviewQuestionSetResponse saveSet(UUID jobId, JobInterviewQuestionSetRequest request) {
        JobPost jobPost = requireJob(jobId);
        int defaultMaxScore = configService.currentGenerationConfig().maxScorePerQuestion();

        Map<UUID, JobInterviewQuestion> existing = new LinkedHashMap<>();
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

    @Override
    @Transactional(readOnly = true)
    public PublicJobInterviewPreviewResponse getPublicPreview(UUID jobId) {
        JobPost jobPost = requirePublicJob(jobId);

        List<JobInterviewQuestion> written =
                questionRepository.findAllByJobPost_IdOrderByDisplayOrderAsc(jobId);

        return toPublicPreview(
                jobPost,
                written,
                configService.currentGenerationConfig().questionCount()
        );
    }

    /** The one place a public preview is shaped, for one job or for a page. */
    private PublicJobInterviewPreviewResponse toPublicPreview(
            JobPost jobPost,
            List<JobInterviewQuestion> written,
            int targetCount
    ) {
        int questionCount = written.size()
                + generatedCount(jobPost.getManualQuestionMode(), written.size(), targetCount);

        return new PublicJobInterviewPreviewResponse(
                jobPost.getId(),
                jobPost.getTitle(),
                questionCount,
                questionCount == 0 ? null : questionCount * MINUTES_PER_QUESTION,
                written.stream()
                        .sorted(Comparator.comparing(JobInterviewQuestion::getDisplayOrder))
                        .map(question -> new PublicJobInterviewQuestionResponse(
                                question.getId(),
                                question.getDisplayOrder(),
                                question.getQuestionType(),
                                question.getQuestionText(),
                                question.getMaxScore()
                        ))
                        .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicJobInterviewPreviewResponse> getPublicPreviews(List<UUID> jobIds) {
        if (jobIds.isEmpty()) return List.of();

        Map<UUID, JobPost> publicJobs = jobPostRepository.findPublicJobsByIds(
                        jobIds,
                        JobStatus.PUBLISHED,
                        VerificationStatus.APPROVED,
                        ProfileStatus.ACTIVE,
                        Instant.now()
                )
                .stream()
                .collect(Collectors.toMap(JobPost::getId, jobPost -> jobPost));

        if (publicJobs.isEmpty()) return List.of();

        // One query for every job's questions, then grouped in memory. Asking
        // per job would put the board's page size straight into the query count.
        Map<UUID, List<JobInterviewQuestion>> byJob =
                questionRepository.findAllByJobPost_IdInOrderByDisplayOrderAsc(List.copyOf(publicJobs.keySet()))
                        .stream()
                        .collect(Collectors.groupingBy(question -> question.getJobPost().getId()));

        int targetCount = configService.currentGenerationConfig().questionCount();

        // The caller's order is the order on screen, so it is preserved rather
        // than handing back whatever order the database chose.
        return jobIds.stream()
                .distinct()
                .map(publicJobs::get)
                .filter(Objects::nonNull)
                .map(jobPost -> toPublicPreview(
                        jobPost,
                        byJob.getOrDefault(jobPost.getId(), List.of()),
                        targetCount
                ))
                .toList();
    }

    private JobPost requireJob(UUID jobId) {
        return jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job post was not found"));
    }

    /**
     * The job as a signed-out visitor may see it.
     *
     * <p>Applies the same test the public job listing applies rather than a
     * looser one of its own: an expired posting, or one whose company was
     * suspended, disappears from the board and its questions go with it.
     */
    private JobPost requirePublicJob(UUID jobId) {
        return jobPostRepository.findPublicJobById(
                        jobId,
                        JobStatus.PUBLISHED,
                        VerificationStatus.APPROVED,
                        ProfileStatus.ACTIVE,
                        Instant.now()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public job was not found"));
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
