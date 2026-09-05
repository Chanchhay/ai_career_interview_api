package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.application.repository.JobApplicationRepository;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AnswerEvaluationInput;
import co.istad.ai_interview_app.features.interview.ai.dto.EvaluatedAnswer;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestion;
import co.istad.ai_interview_app.features.interview.ai.dto.InterviewEvaluationRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.InterviewEvaluationResult;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewAnswer;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewFeedback;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewQuestion;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import co.istad.ai_interview_app.features.interview.ai.mapper.AiInterviewMapper;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewAnswerRepository;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewFeedbackRepository;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewQuestionRepository;
import co.istad.ai_interview_app.features.interview.question.repository.JobInterviewQuestionRepository;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewSessionRepository;
import co.istad.ai_interview_app.features.interview.vapi.dto.TranscriptSegmentationRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.TranscriptSegmentationResult;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiCallBindingRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiTranscriptTurn;
import co.istad.ai_interview_app.features.interview.vapi.dto.VoiceTranscriptRequest;
import co.istad.ai_interview_app.features.interview.vapi.service.AiInterviewTranscriptSegmenter;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.JobPostSection;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.features.moderator.entity.CandidateApplicationReview;
import co.istad.ai_interview_app.features.moderator.repository.CandidateApplicationReviewRepository;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.repository.JobSeekerProfileRepository;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import org.springframework.context.ApplicationEventPublisher;
import co.istad.ai_interview_app.features.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInterviewServiceImpl implements AiInterviewService {

    private final InterviewQuestionComposer questionComposer;
    private final AiInterviewConfigService configService;
    private final AiInterviewEvaluator evaluator;
    private final JobPostRepository jobPostRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final AiInterviewSessionRepository sessionRepository;
    private final CandidateApplicationReviewRepository reviewRepository;
    private final AiInterviewQuestionRepository questionRepository;
    private final JobInterviewQuestionRepository writtenQuestionRepository;
    private final AiInterviewAnswerRepository answerRepository;
    private final AiInterviewFeedbackRepository feedbackRepository;
    private final AiInterviewMapper mapper;
    private final AiInterviewTranscriptSegmenter transcriptSegmenter;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher events;

    @Value("${spring.ai.google.genai.chat.model:gemini}")
    private String aiModel;

    private static final Set<InterviewStatus> ACTIVE_APPLICATION_INTERVIEW_STATUSES = Set.of(
            InterviewStatus.PREPARING,
            InterviewStatus.READY,
            InterviewStatus.PENDING,
            InterviewStatus.IN_PROGRESS
    );

    @Override
    public AiInterviewSessionResponse createInterviewForJob(Long jobId) {
        return fillSession(transactionTemplate.execute(status -> createPreparingSession(jobId)));
    }

    @Override
    public AiInterviewSessionResponse createInterviewForApplication(Long applicationId) {
        return fillSession(transactionTemplate.execute(status -> createPreparingApplicationSession(applicationId)));
    }

    /**
     * Turns a PREPARING session into a READY one by giving it its questions.
     *
     * <p>Shared by the practice and application entry points so a candidate is
     * asked the same thing either way — the two used to hold identical copies of
     * this, which is exactly how they would come to differ.
     */
    private AiInterviewSessionResponse fillSession(GenerationContext context) {
        // Read once, so the interview that is generated and the interview that
        // is validated are the same one even if an admin saves new settings
        // while Gemini is still answering.
        AiInterviewGenerationConfig config = configService.currentGenerationConfig();

        List<GeneratedQuestion> questions;
        try {
            questions = questionComposer.compose(
                    new InterviewQuestionComposer.JobFacts(
                            context.jobTitle(),
                            context.jobDescription(),
                            context.experienceLevel(),
                            context.requiredSkills()
                    ),
                    context.manualQuestionMode(),
                    context.writtenQuestions(),
                    config
            );
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markSessionFailed(context.sessionId()));
            throw ex;
        }

        return transactionTemplate.execute(status -> persistQuestions(context.sessionId(), questions));
    }


    @Override
    public List<AiInterviewSessionResponse> getMyInterviews() {
        return transactionTemplate.execute(status -> sessionRepository
                .findAllByJobSeeker_KeycloakUserIdOrderByCreatedAtDesc(AuthUtils.extractUserId())
                .stream()
                .map(mapper::toSessionResponse)
                .toList());
    }

    @Override
    public AiInterviewSessionResponse getMyInterview(Long sessionId) {
        return transactionTemplate.execute(status -> mapper.toSessionResponse(resolveMySessionWithQuestions(sessionId)));
    }

    @Override
    public AiInterviewSessionResponse startInterview(Long sessionId) {
        return transactionTemplate.execute(status ->
                startSession(resolveMySessionWithQuestions(sessionId)));
    }

    @Override
    public AiInterviewSessionResponse submitAnswer(
            Long sessionId,
            Long questionId,
            AiInterviewAnswerRequest request
    ) {
        return transactionTemplate.execute(status -> {
            recordAnswer(resolveMySessionWithQuestions(sessionId), questionId, request);

            return mapper.toSessionResponse(resolveMySessionWithQuestions(sessionId));
        });
    }

    /**
     * Opens an interview that is ready to be sat.
     *
     * <p>Takes a session the caller has already established belongs to whoever
     * is asking, so the candidate flow and the guest flow share one set of
     * rules about what may be started and when.
     */
    private AiInterviewSessionResponse startSession(AiInterviewSession session) {
        if (session.getStatus() == InterviewStatus.IN_PROGRESS
                || session.getStatus() == InterviewStatus.COMPLETED) {
            return mapper.toSessionResponse(session);
        }

        if (session.getStatus() != InterviewStatus.READY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only ready interviews can be started");
        }

        session.setStatus(InterviewStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());

        return mapper.toSessionResponse(session);
    }

    /**
     * Records an answer against a question of an already-resolved session.
     *
     * <p>The question is looked up within the session rather than by a query
     * that re-checks the owner: the session was resolved by owner a moment ago,
     * and a question of that session is by definition the same person's.
     */
    private void recordAnswer(
            AiInterviewSession session,
            Long questionId,
            AiInterviewAnswerRequest request
    ) {
        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interview must be in progress before answers can be submitted"
            );
        }

        AiInterviewQuestion question = session.getQuestions()
                .stream()
                .filter(candidate -> candidate.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview question was not found for this interview"
                ));

        AiInterviewAnswer answer = answerRepository.findByQuestion_Id(questionId)
                .orElseGet(() -> {
                    AiInterviewAnswer newAnswer = new AiInterviewAnswer();
                    newAnswer.setQuestion(question);
                    question.getAnswers().add(newAnswer);
                    return newAnswer;
                });

        answer.setAnswerText(normalizeBlankToNull(request.answerText()));
        answer.setScore(null);
        answer.setFeedback(null);
        answer.setModelAnswer(null);
        answerRepository.save(answer);
    }

    @Override
    public AiInterviewResultResponse completeInterview(Long sessionId) {
        EvaluationContext context = transactionTemplate.execute(status -> prepareEvaluation(sessionId));
        if (context.alreadyCompleted()) {
            return transactionTemplate.execute(status -> mapper.toResultResponse(resolveMySessionWithResult(sessionId)));
        }

        InterviewEvaluationResult evaluation;
        try {
            evaluation = evaluator.evaluate(context.request());
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markSessionFailed(sessionId));
            throw ex;
        }

        try {
            return transactionTemplate.execute(status -> persistEvaluation(sessionId, evaluation));
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markSessionFailed(sessionId));
            throw ex;
        }
    }

    @Override
    public AiInterviewResultResponse getResult(Long sessionId) {
        return transactionTemplate.execute(status -> {
            AiInterviewSession session = resolveMySessionWithResult(sessionId);
            if (session.getStatus() != InterviewStatus.COMPLETED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview result is not ready yet");
            }
            return mapper.toResultResponse(session);
        });
    }

    @Override
    public AiInterviewSessionResponse bindVapiCall(Long sessionId, VapiCallBindingRequest request) {
        return transactionTemplate.execute(status ->
                bindCall(resolveMySessionWithQuestions(sessionId), request));
    }

    /**
     * Attaches a voice call to an interview the caller has already been shown to
     * own.
     *
     * <p>Shared by the candidate and guest flows: which call may voice which
     * interview is the same question whoever is sitting it.
     */
    private AiInterviewSessionResponse bindCall(AiInterviewSession session, VapiCallBindingRequest request) {
        {
            Long sessionId = session.getId();

            if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Interview must be in progress before a voice call can be attached"
                );
            }

            String callId = normalizeBlankToNull(request.callId());
            if (callId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vapi call id is required");
            }

            // The call id is what the webhook authenticates against, so it must
            // never point at two interviews.
            if (sessionRepository.existsByCallSessionIdAndIdNot(callId, sessionId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This voice call is already bound to another interview");
            }

            session.setCallSessionId(callId);

            return mapper.toSessionResponse(session);
        }
    }

    @Override
    public void completeFromVapiTranscript(
            String vapiCallId,
            String transcript,
            List<VapiTranscriptTurn> turns
    ) {
        Long sessionId = transactionTemplate.execute(status -> {
            Optional<AiInterviewSession> found =
                    sessionRepository.findWithQuestionsByCallSessionId(vapiCallId);

            if (found.isEmpty()) {
                log.warn("Vapi call={} does not match any interview session", vapiCallId);
                return null;
            }

            return acceptTranscript(found.get(), transcript, turns);
        });

        if (sessionId == null) {
            return;
        }

        // Errors are swallowed: rethrowing would 500 the webhook and earn a
        // retry of an event that will fail the same way.
        scoreFromTranscript(sessionId, false);
    }

    @Override
    public AiInterviewSessionResponse submitVoiceTranscript(
            Long sessionId,
            VoiceTranscriptRequest request
    ) {
        return submitVoiceTranscript(sessionId, request, this::resolveMySessionWithQuestions);
    }

    /**
     * Stores and scores a finished call, for whoever the resolver says owns it.
     *
     * <p>The resolver is the only difference between a candidate submitting a
     * transcript and a guest doing it, so it is the only thing passed in.
     */
    private AiInterviewSessionResponse submitVoiceTranscript(
            Long sessionId,
            VoiceTranscriptRequest request,
            java.util.function.LongFunction<AiInterviewSession> resolve
    ) {
        List<VapiTranscriptTurn> turns = request.turns()
                .stream()
                .map(turn -> new VapiTranscriptTurn(
                        VapiTranscriptTurn.normalizeRole(turn.role()),
                        turn.text()
                ))
                .filter(turn -> turn.role() != null)
                .toList();

        Long readySessionId = transactionTemplate.execute(status -> acceptTranscript(
                resolve.apply(sessionId),
                VapiTranscriptTurn.toTranscript(turns),
                turns
        ));

        if (readySessionId != null) {
            // The candidate is waiting on this response, so failures surface as
            // an error rather than a silently unscored interview.
            scoreFromTranscript(readySessionId, true);
        }

        return transactionTemplate.execute(status ->
                mapper.toSessionResponse(resolve.apply(sessionId)));
    }

    /**
     * Stores a finished call's transcript against its session.
     *
     * <p>Deliberately does not touch the answers. Splitting the transcript is a
     * Gemini call, which must not run inside this transaction.
     *
     * @return the session id when the interview is ready to be split and scored,
     * or {@code null} when it is not — already scored, or not in progress.
     */
    private Long acceptTranscript(
            AiInterviewSession session,
            String transcript,
            List<VapiTranscriptTurn> turns
    ) {
        if (hasText(transcript)) {
            session.setTranscript(transcript);
        } else if (!turns.isEmpty()) {
            session.setTranscript(VapiTranscriptTurn.toTranscript(turns));
        }

        if (session.getStatus() == InterviewStatus.COMPLETED) {
            // The webhook and the browser both report the same call ending, and
            // Vapi retries. Whichever arrives second finds the work already done.
            log.info("Transcript for session={} arrived after it was scored", session.getId());
            return null;
        }

        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            log.warn(
                    "Transcript for session={} in status={}; not scoring",
                    session.getId(), session.getStatus()
            );
            return null;
        }

        if (!hasText(session.getTranscript())) {
            log.warn("Transcript for session={} was empty; not scoring", session.getId());
            return null;
        }

        return session.getId();
    }

    /**
     * Splits the stored transcript into answers, then scores the interview.
     *
     * <p>Two Gemini calls, each sandwiched between short transactions, because
     * neither may hold one open while waiting on the network.
     *
     * @param propagateErrors whether a failure should reach the caller, or be
     *                        logged and left for a later attempt
     */
    private void scoreFromTranscript(Long sessionId, boolean propagateErrors) {
        try {
            TranscriptSegmentationRequest segmentationRequest = transactionTemplate.execute(status ->
                    buildSegmentationRequest(sessionId));

            TranscriptSegmentationResult segmentation =
                    transcriptSegmenter.segment(segmentationRequest);

            List<Integer> unanswered = transactionTemplate.execute(status ->
                    applySegmentedAnswers(sessionId, segmentation));

            if (unanswered == null || !unanswered.isEmpty()) {
                // Left IN_PROGRESS on purpose: the candidate can still answer the
                // remaining questions by typing rather than being scored on an
                // interview they did not finish.
                log.warn(
                        "Session={} transcript left questions {} unanswered; leaving the interview open",
                        sessionId, unanswered
                );
                return;
            }

            EvaluationContext context = transactionTemplate.execute(status ->
                    prepareEvaluation(requireSession(sessionId)));

            if (context == null || context.alreadyCompleted()) {
                return;
            }

            InterviewEvaluationResult evaluation = evaluator.evaluate(context.request());

            transactionTemplate.executeWithoutResult(status ->
                    persistEvaluation(requireSessionWithResult(sessionId), evaluation));

            log.info("Scored interview session={} from its transcript", sessionId);
        } catch (RuntimeException ex) {
            log.error("Scoring session={} from its transcript failed", sessionId, ex);
            transactionTemplate.executeWithoutResult(status -> markSessionFailed(sessionId));
            if (propagateErrors) {
                throw ex;
            }
        }
    }

    private TranscriptSegmentationRequest buildSegmentationRequest(Long sessionId) {
        AiInterviewSession session = requireSession(sessionId);

        List<TranscriptSegmentationRequest.TranscriptQuestion> questions = session.getQuestions()
                .stream()
                .sorted(Comparator.comparing(AiInterviewQuestion::getDisplayOrder))
                .map(question -> new TranscriptSegmentationRequest.TranscriptQuestion(
                        question.getId(),
                        question.getDisplayOrder(),
                        question.getQuestionText()
                ))
                .toList();

        return new TranscriptSegmentationRequest(questions, session.getTranscript());
    }

    /**
     * @return display orders of the questions the transcript did not answer,
     * empty when the interview is complete enough to score
     */
    private List<Integer> applySegmentedAnswers(
            Long sessionId,
            TranscriptSegmentationResult segmentation
    ) {
        AiInterviewSession session = requireSession(sessionId);

        Map<Long, String> answersByQuestionId = segmentation.answers()
                .stream()
                .filter(answer -> !Boolean.FALSE.equals(answer.answered()))
                .filter(answer -> normalizeBlankToNull(answer.answerText()) != null)
                .collect(Collectors.toMap(
                        TranscriptSegmentationResult.SegmentedAnswer::questionId,
                        answer -> normalizeBlankToNull(answer.answerText())
                ));

        // The transcript is the record of what was said, so it replaces anything
        // already stored for a question it covered. A question it did not cover
        // keeps whatever the candidate typed.
        session.getQuestions().forEach(question -> {
            String spoken = answersByQuestionId.get(question.getId());
            if (spoken == null) {
                return;
            }

            AiInterviewAnswer answer = answerRepository.findByQuestion_Id(question.getId())
                    .orElseGet(() -> {
                        AiInterviewAnswer newAnswer = new AiInterviewAnswer();
                        newAnswer.setQuestion(question);
                        question.getAnswers().add(newAnswer);
                        return newAnswer;
                    });

            answer.setAnswerText(spoken);
            answer.setScore(null);
            answer.setFeedback(null);
            answer.setModelAnswer(null);
            answerRepository.save(answer);
        });

        // Sessions keep whatever length they were generated at, so this asks
        // only that the session has questions — not that it matches the count
        // an admin has configured since.
        if (session.getQuestions().isEmpty()) {
            log.warn("Session={} has no questions", sessionId);
            return null;
        }

        return session.getQuestions()
                .stream()
                .sorted(Comparator.comparing(AiInterviewQuestion::getDisplayOrder))
                .filter(question -> question.getAnswers()
                        .stream()
                        .findFirst()
                        .map(answer -> normalizeBlankToNull(answer.getAnswerText()))
                        .isEmpty())
                .map(AiInterviewQuestion::getDisplayOrder)
                .toList();
    }

    private AiInterviewSession requireSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "AI interview session was not found"
                ));
    }

    private AiInterviewSession requireSessionWithResult(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "AI interview session was not found"
                ));
    }

    private GenerationContext createPreparingSession(Long jobId) {
        JobSeekerProfile jobSeekerProfile = resolveMyJobSeekerProfile();
        JobPost jobPost = jobPostRepository.findByIdAndStatus(jobId, JobStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published job was not found"));

        AiInterviewSession session = new AiInterviewSession();
        /*
         * Attached to the seeker's application for this job when they have one.
         *
         * <p>This entry point exists for practising against a job nobody has
         * applied to, and it used to hard-code a null application. But the same
         * button is reachable after applying, and an interview that is not
         * attached is invisible to the moderator queue and cannot satisfy the
         * approval check — so a candidate who practised, applied, and sat the
         * interview from the job page could never be approved. Which button was
         * pressed should not decide that.
         */
        session.setApplication(
                applicationRepository
                        .findLiveApplication(jobPost.getId(), jobSeekerProfile.getId())
                        .orElse(null)
        );
        session.setJobPost(jobPost);
        session.setJobSeeker(jobSeekerProfile.getUserAccount());
        session.setProvider("GEMINI");
        session.setAiModel(aiModel);
        session.setStatus(InterviewStatus.PREPARING);

        AiInterviewSession savedSession = sessionRepository.save(session);

        return new GenerationContext(
                savedSession.getId(),
                jobPost.getTitle(),
                buildJobDescription(jobPost),
                jobPost.getExperienceLevel(),
                requiredSkills(jobPost),
                jobPost.getManualQuestionMode(),
                questionComposer.writtenQuestions(jobPost.getId())
        );
    }

    private GenerationContext createPreparingApplicationSession(Long applicationId) {
        JobApplication application = applicationRepository
                .findByIdAndJobSeekerProfile_UserAccount_KeycloakUserId(applicationId, AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application was not found for authenticated job seeker"
                ));

        if (application.getStatus() == ApplicationStatus.WITHDRAWN
                || application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.HIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This application cannot start an AI interview");
        }
        /*
         * Whether an interview already happened is a question about sessions,
         * not about the application's status.
         *
         * <p>This used to infer it from the status alone, which let the two
         * checks disagree: an application sitting at MODERATOR_REVIEW_PENDING
         * with no completed session attached refused a new interview on the
         * grounds that one existed, while approval refused on the grounds that
         * none did. The candidate could neither interview nor be approved, and
         * both messages were true only of the other check's view of the world.
         *
         * <p>Reading the session makes the two agree by construction, and lets
         * an application whose status drifted recover by simply interviewing.
         */
        if (sessionRepository
                .findFirstByApplication_IdAndStatusOrderByEndedAtDesc(
                        application.getId(),
                        InterviewStatus.COMPLETED
                )
                .isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This application already has a completed AI interview"
            );
        }

        // Past the interview stage entirely. Distinct from the check above so
        // the refusal says what is actually true of the application.
        if (application.getStatus() == ApplicationStatus.SHORTLISTED
                || application.getStatus() == ApplicationStatus.HUMAN_INTERVIEW_SCHEDULED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This application has already moved past the AI interview stage"
            );
        }
        if (sessionRepository.existsByApplication_IdAndStatusIn(application.getId(), ACTIVE_APPLICATION_INTERVIEW_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application already has an active AI interview");
        }

        JobPost jobPost = application.getJobPost();
        AiInterviewSession session = new AiInterviewSession();
        session.setApplication(application);
        session.setJobPost(jobPost);
        session.setJobSeeker(application.getJobSeekerProfile().getUserAccount());
        session.setProvider("GEMINI");
        session.setAiModel(aiModel);
        session.setStatus(InterviewStatus.PREPARING);
        application.setStatus(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS);

        AiInterviewSession savedSession = sessionRepository.save(session);

        return new GenerationContext(
                savedSession.getId(),
                jobPost.getTitle(),
                buildJobDescription(jobPost),
                jobPost.getExperienceLevel(),
                requiredSkills(jobPost),
                jobPost.getManualQuestionMode(),
                questionComposer.writtenQuestions(jobPost.getId())
        );
    }

    /**
     * Writes the composed questions onto the session and opens it.
     *
     * <p>Takes an already-checked list rather than a raw AI response: the set
     * may be part hand-written, and only the generated part is the AI's word to
     * doubt. It is validated where it is generated.
     */
    private AiInterviewSessionResponse persistQuestions(
            Long sessionId,
            List<GeneratedQuestion> questions
    ) {
        AiInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview session was not found"));

        session.getQuestions().clear();
        for (GeneratedQuestion generatedQuestion : questions) {
            AiInterviewQuestion question = new AiInterviewQuestion();
            question.setSession(session);
            question.setDisplayOrder(generatedQuestion.order());
            question.setQuestionType(generatedQuestion.type());
            question.setQuestionText(normalizeBlankToNull(generatedQuestion.question()));
            question.setExpectedAnswer(normalizeBlankToNull(generatedQuestion.rubric()));
            question.setMaxScore(generatedQuestion.maxScore());
            session.getQuestions().add(question);
        }

        session.setStatus(InterviewStatus.READY);
        sessionRepository.flush();

        return mapper.toSessionResponse(session);
    }

    private EvaluationContext prepareEvaluation(Long sessionId) {
        return prepareEvaluation(resolveMySessionWithQuestions(sessionId));
    }

    /**
     * Builds the Gemini request for a session that has already been resolved.
     *
     * <p>Split from the seeker-facing overload so the Vapi webhook, which has no
     * authenticated caller to resolve by, scores interviews through exactly the
     * same rules rather than a parallel copy of them.
     */
    private EvaluationContext prepareEvaluation(AiInterviewSession session) {
        if (session.getStatus() == InterviewStatus.COMPLETED) {
            return new EvaluationContext(null, true);
        }

        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview must be in progress before it can be completed");
        }

        if (session.getQuestions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview questions are not ready");
        }

        List<AnswerEvaluationInput> answers = session.getQuestions()
                .stream()
                .sorted(Comparator.comparing(AiInterviewQuestion::getDisplayOrder))
                .map(this::toEvaluationInput)
                .toList();

        return new EvaluationContext(
                new InterviewEvaluationRequest(
                        session.getJobPost().getTitle(),
                        buildJobDescription(session.getJobPost()),
                        session.getJobPost().getExperienceLevel(),
                        requiredSkills(session.getJobPost()),
                        answers
                ),
                false
        );
    }

    private AiInterviewResultResponse persistEvaluation(
            Long sessionId,
            InterviewEvaluationResult evaluation
    ) {
        return persistEvaluation(resolveMySessionWithResult(sessionId), evaluation);
    }

    private AiInterviewResultResponse persistEvaluation(
            AiInterviewSession session,
            InterviewEvaluationResult evaluation
    ) {
        if (session.getStatus() == InterviewStatus.COMPLETED) {
            return mapper.toResultResponse(session);
        }

        validateEvaluation(session, evaluation);

        Map<Long, EvaluatedAnswer> evaluatedAnswersByQuestionId = evaluation.answers()
                .stream()
                .collect(Collectors.toMap(EvaluatedAnswer::questionId, Function.identity()));

        session.getQuestions().forEach(question -> {
            AiInterviewAnswer answer = question.getAnswers()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Interview cannot be completed until all questions are answered"
                    ));

            EvaluatedAnswer evaluatedAnswer = evaluatedAnswersByQuestionId.get(question.getId());
            answer.setScore(evaluatedAnswer.score());
            answer.setFeedback(normalizeBlankToNull(evaluatedAnswer.feedback()));
            answer.setModelAnswer(normalizeBlankToNull(evaluatedAnswer.modelAnswer()));
        });

        AiInterviewFeedback feedback = feedbackRepository.findBySession_Id(session.getId())
                .orElseGet(() -> {
                    AiInterviewFeedback newFeedback = new AiInterviewFeedback();
                    newFeedback.setSession(session);
                    session.setFeedback(newFeedback);
                    return newFeedback;
                });

        feedback.setCommunicationScore(evaluation.communicationScore());
        feedback.setTechnicalScore(evaluation.technicalScore());
        feedback.setConfidenceScore(evaluation.confidenceScore());
        feedback.setProblemSolvingScore(evaluation.problemSolvingScore());
        feedback.setOverallScore(evaluation.overallScore());
        feedback.setStrengths(normalizeBlankToNull(evaluation.strengths()));
        feedback.setWeaknesses(normalizeBlankToNull(evaluation.weaknesses()));
        feedback.setRecommendation(normalizeBlankToNull(evaluation.recommendation()));

        session.setTotalScore(evaluation.overallScore());
        session.setResult(evaluation.result());
        session.setStatus(InterviewStatus.COMPLETED);
        session.setEndedAt(Instant.now());

        if (session.getApplication() != null) {
            session.getApplication().setStatus(ApplicationStatus.MODERATOR_REVIEW_PENDING);
            reviewRepository.findByApplication_Id(session.getApplication().getId())
                    .orElseGet(() -> {
                        CandidateApplicationReview review = new CandidateApplicationReview();
                        review.setApplication(session.getApplication());
                        review.setReviewStatus(CandidateApplicationReviewStatus.PENDING);
                        return reviewRepository.save(review);
                    });
        }

        /*
         * Published here rather than at either call site: this is the one place
         * a session becomes COMPLETED with a score, and it is reached from the
         * manual completion path and both Vapi transcript paths alike.
         */
        events.publishEvent(new NotificationEvents.AiInterviewCompleted(session.getId()));

        return mapper.toResultResponse(session);
    }

    private AnswerEvaluationInput toEvaluationInput(AiInterviewQuestion question) {
        AiInterviewAnswer answer = question.getAnswers()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Interview cannot be completed until all questions are answered"
                ));

        String answerText = normalizeBlankToNull(answer.getAnswerText());
        if (answerText == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interview cannot be completed until all questions are answered"
            );
        }

        return new AnswerEvaluationInput(
                question.getId(),
                question.getDisplayOrder(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getExpectedAnswer(),
                question.getMaxScore(),
                answerText
        );
    }

    private JobSeekerProfile resolveMyJobSeekerProfile() {
        return jobSeekerProfileRepository.findByUserAccount_KeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job seeker profile was not found for authenticated user"
                ));
    }

    private AiInterviewSession resolveMySessionWithQuestions(Long sessionId) {
        return sessionRepository.findWithQuestionsByIdAndJobSeeker_KeycloakUserId(sessionId, AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "AI interview session was not found for authenticated job seeker"
                ));
    }

    private AiInterviewSession resolveMySessionWithResult(Long sessionId) {
        return sessionRepository.findWithResultByIdAndJobSeeker_KeycloakUserId(sessionId, AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "AI interview session was not found for authenticated job seeker"
                ));
    }

    private void markSessionFailed(Long sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus(InterviewStatus.FAILED);
            if (session.getApplication() != null) {
                session.getApplication().setStatus(ApplicationStatus.AI_INTERVIEW_FAILED);
            }
        });
    }

    private String buildJobDescription(JobPost jobPost) {
        String sectionText = jobPost.getSections()
                .stream()
                .sorted(Comparator.comparing(JobPostSection::getDisplayOrder))
                .flatMap(section -> Stream.of(section.getTitle(), section.getContentText(), section.getContentMarkdown()))
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n\n"));

        return Stream.of(jobPost.getDescription(), sectionText)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private List<String> requiredSkills(JobPost jobPost) {
        return jobPost.getSkills()
                .stream()
                .map(jobPostSkill -> jobPostSkill.getSkill().getName())
                .filter(skill -> skill != null && !skill.isBlank())
                .sorted()
                .toList();
    }


    private void validateEvaluation(
            AiInterviewSession session,
            InterviewEvaluationResult evaluation
    ) {
        if (evaluation == null
                || evaluation.answers() == null
                || evaluation.answers().size() != session.getQuestions().size()
                || evaluation.result() == null
                || normalizeBlankToNull(evaluation.strengths()) == null
                || normalizeBlankToNull(evaluation.weaknesses()) == null
                || normalizeBlankToNull(evaluation.recommendation()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned invalid interview evaluation");
        }

        validateScore(evaluation.communicationScore());
        validateScore(evaluation.technicalScore());
        validateScore(evaluation.confidenceScore());
        validateScore(evaluation.problemSolvingScore());
        validateScore(evaluation.overallScore());

        Set<Long> questionIds = session.getQuestions()
                .stream()
                .map(AiInterviewQuestion::getId)
                .collect(Collectors.toSet());

        Set<Long> evaluatedQuestionIds = new HashSet<>();
        for (EvaluatedAnswer answer : evaluation.answers()) {
            if (answer == null
                    || answer.questionId() == null
                    || !questionIds.contains(answer.questionId())
                    || !evaluatedQuestionIds.add(answer.questionId())
                    || normalizeBlankToNull(answer.feedback()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned invalid answer evaluations");
            }
            validateScore(answer.score());
        }
    }

    private void validateScore(BigDecimal score) {
        if (score == null
                || score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(BigDecimal.TEN) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an invalid score");
        }
    }

    /**
     * Everything the generation step needs, read inside the transaction that
     * created the session.
     *
     * <p>The written questions ride along rather than being fetched later: the
     * AI call happens outside any transaction, and reading a job's question bank
     * out there would either lazy-load on a closed session or race an
     * administrator saving new questions mid-generation.
     */
    private record GenerationContext(
            Long sessionId,
            String jobTitle,
            String jobDescription,
            String experienceLevel,
            List<String> requiredSkills,
            ManualQuestionMode manualQuestionMode,
            List<GeneratedQuestion> writtenQuestions
    ) {
    }

    private record EvaluationContext(
            InterviewEvaluationRequest request,
            boolean alreadyCompleted
    ) {
    }

    /* ------------------------------------------------------------ guests --- */

    @Override
    public AiInterviewSessionResponse createGuestInterview(
            Long jobId,
            String guestToken,
            String guestIpHash,
            ManualQuestionMode modeOverride
    ) {
        GenerationContext context = transactionTemplate.execute(status ->
                createPreparingGuestSession(jobId, guestToken, guestIpHash, modeOverride));

        return fillSession(context);
    }

    /**
     * A guest's session: no account, no application, and a token instead.
     *
     * <p>Only published jobs, the same rule the public listing applies. A guest
     * must not be able to interview against a draft by guessing its id.
     */
    private GenerationContext createPreparingGuestSession(
            Long jobId,
            String guestToken,
            String guestIpHash,
            ManualQuestionMode modeOverride
    ) {
        JobPost jobPost = jobPostRepository.findByIdAndStatus(jobId, JobStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published job was not found"));

        AiInterviewSession session = new AiInterviewSession();
        session.setJobPost(jobPost);
        // No jobSeeker and no application on purpose: this interview belongs to
        // nobody the platform knows, and must never reach a moderator's queue.
        session.setGuestToken(guestToken);
        session.setGuestIpHash(guestIpHash);
        session.setProvider("GEMINI");
        session.setAiModel(aiModel);
        session.setStatus(InterviewStatus.PREPARING);

        AiInterviewSession savedSession = sessionRepository.save(session);

        InterviewQuestionComposer.JobFacts facts = questionComposer.jobFacts(jobPost);

        return new GenerationContext(
                savedSession.getId(),
                facts.title(),
                facts.description(),
                facts.experienceLevel(),
                facts.requiredSkills(),
                modeOverride == null ? jobPost.getManualQuestionMode() : modeOverride,
                questionComposer.writtenQuestions(jobPost.getId())
        );
    }

    @Override
    public AiInterviewSessionResponse getGuestInterview(Long sessionId, String guestToken) {
        return transactionTemplate.execute(status ->
                mapper.toSessionResponse(resolveGuestSession(sessionId, guestToken)));
    }

    @Override
    public AiInterviewSessionResponse startGuestInterview(Long sessionId, String guestToken) {
        return transactionTemplate.execute(status ->
                startSession(resolveGuestSession(sessionId, guestToken)));
    }

    @Override
    public AiInterviewSessionResponse submitGuestAnswer(
            Long sessionId,
            Long questionId,
            String guestToken,
            AiInterviewAnswerRequest request
    ) {
        return transactionTemplate.execute(status -> {
            AiInterviewSession session = resolveGuestSession(sessionId, guestToken);
            recordAnswer(session, questionId, request);

            return mapper.toSessionResponse(resolveGuestSession(sessionId, guestToken));
        });
    }

    @Override
    public AiInterviewResultResponse completeGuestInterview(Long sessionId, String guestToken) {
        EvaluationContext context = transactionTemplate.execute(status ->
                prepareEvaluation(resolveGuestSession(sessionId, guestToken)));

        if (context.alreadyCompleted()) {
            return transactionTemplate.execute(status ->
                    mapper.toResultResponse(resolveGuestSessionWithResult(sessionId, guestToken)));
        }

        InterviewEvaluationResult evaluation;
        try {
            evaluation = evaluator.evaluate(context.request());
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markSessionFailed(sessionId));
            throw ex;
        }

        return transactionTemplate.execute(status ->
                persistEvaluation(resolveGuestSessionWithResult(sessionId, guestToken), evaluation));
    }


    @Override
    public AiInterviewSessionResponse bindGuestVapiCall(
            Long sessionId,
            String guestToken,
            VapiCallBindingRequest request
    ) {
        return transactionTemplate.execute(status ->
                bindCall(resolveGuestSession(sessionId, guestToken), request));
    }

    @Override
    public AiInterviewSessionResponse submitGuestVoiceTranscript(
            Long sessionId,
            String guestToken,
            VoiceTranscriptRequest request
    ) {
        return submitVoiceTranscript(sessionId, request, id -> resolveGuestSession(id, guestToken));
    }

    @Override
    public AiInterviewResultResponse getGuestResult(Long sessionId, String guestToken) {
        return transactionTemplate.execute(status ->
                mapper.toResultResponse(resolveGuestSessionWithResult(sessionId, guestToken)));
    }

    /*
     * The token is the whole of a guest's authorisation, so it is matched in the
     * query rather than fetched and compared: a session that does not belong to
     * this token is simply not found, and answers the same as one that does not
     * exist.
     */

    private AiInterviewSession resolveGuestSession(Long sessionId, String guestToken) {
        if (guestToken == null || guestToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest interview was not found");
        }

        return sessionRepository.findWithQuestionsByIdAndGuestToken(sessionId, guestToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guest interview was not found"
                ));
    }

    private AiInterviewSession resolveGuestSessionWithResult(Long sessionId, String guestToken) {
        if (guestToken == null || guestToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest interview was not found");
        }

        return sessionRepository.findWithResultByIdAndGuestToken(sessionId, guestToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guest interview was not found"
                ));
    }
}
