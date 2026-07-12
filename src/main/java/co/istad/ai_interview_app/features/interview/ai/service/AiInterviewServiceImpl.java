package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.application.repository.JobApplicationRepository;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AnswerEvaluationInput;
import co.istad.ai_interview_app.features.interview.ai.dto.EvaluatedAnswer;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestion;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestionSet;
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
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewSessionRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class AiInterviewServiceImpl implements AiInterviewService {

    private static final int QUESTION_COUNT = 7;

    private final AiInterviewQuestionGenerator questionGenerator;
    private final AiInterviewEvaluator evaluator;
    private final JobPostRepository jobPostRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final AiInterviewSessionRepository sessionRepository;
    private final CandidateApplicationReviewRepository reviewRepository;
    private final AiInterviewQuestionRepository questionRepository;
    private final AiInterviewAnswerRepository answerRepository;
    private final AiInterviewFeedbackRepository feedbackRepository;
    private final AiInterviewMapper mapper;
    private final TransactionTemplate transactionTemplate;

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
        GenerationContext context = transactionTemplate.execute(status -> createPreparingSession(jobId));

        GeneratedQuestionSet generatedQuestions;
        try {
            generatedQuestions = questionGenerator.generateQuestions(
                    context.jobTitle(),
                    context.jobDescription(),
                    context.experienceLevel(),
                    context.requiredSkills()
            );
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markSessionFailed(context.sessionId()));
            throw ex;
        }

        return transactionTemplate.execute(status -> persistGeneratedQuestions(context.sessionId(), generatedQuestions));
    }

    @Override
    public AiInterviewSessionResponse createInterviewForApplication(Long applicationId) {
        GenerationContext context = transactionTemplate.execute(status -> createPreparingApplicationSession(applicationId));

        GeneratedQuestionSet generatedQuestions;
        try {
            generatedQuestions = questionGenerator.generateQuestions(
                    context.jobTitle(),
                    context.jobDescription(),
                    context.experienceLevel(),
                    context.requiredSkills()
            );
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markSessionFailed(context.sessionId()));
            throw ex;
        }

        return transactionTemplate.execute(status -> persistGeneratedQuestions(context.sessionId(), generatedQuestions));
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
        return transactionTemplate.execute(status -> {
            AiInterviewSession session = resolveMySessionWithQuestions(sessionId);

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
        });
    }

    @Override
    public AiInterviewSessionResponse submitAnswer(
            Long sessionId,
            Long questionId,
            AiInterviewAnswerRequest request
    ) {
        return transactionTemplate.execute(status -> {
            AiInterviewSession session = resolveMySessionWithQuestions(sessionId);
            if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview must be in progress before answers can be submitted");
            }

            AiInterviewQuestion question = questionRepository
                    .findByIdAndSession_IdAndSession_JobSeeker_KeycloakUserId(
                            questionId,
                            sessionId,
                            AuthUtils.extractUserId()
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Interview question was not found for authenticated job seeker"
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
            answerRepository.save(answer);

            return mapper.toSessionResponse(resolveMySessionWithQuestions(sessionId));
        });
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

    private GenerationContext createPreparingSession(Long jobId) {
        JobSeekerProfile jobSeekerProfile = resolveMyJobSeekerProfile();
        JobPost jobPost = jobPostRepository.findByIdAndStatus(jobId, JobStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published job was not found"));

        AiInterviewSession session = new AiInterviewSession();
        session.setApplication(null);
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
                requiredSkills(jobPost)
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
        if (application.getStatus() == ApplicationStatus.MODERATOR_REVIEW_PENDING
                || application.getStatus() == ApplicationStatus.SHORTLISTED
                || application.getStatus() == ApplicationStatus.HUMAN_INTERVIEW_SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application already has a completed AI interview");
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
                requiredSkills(jobPost)
        );
    }

    private AiInterviewSessionResponse persistGeneratedQuestions(
            Long sessionId,
            GeneratedQuestionSet generatedQuestionSet
    ) {
        validateGeneratedQuestions(generatedQuestionSet);

        AiInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview session was not found"));

        session.getQuestions().clear();
        for (GeneratedQuestion generatedQuestion : generatedQuestionSet.questions()) {
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
        AiInterviewSession session = resolveMySessionWithQuestions(sessionId);

        if (session.getStatus() == InterviewStatus.COMPLETED) {
            return new EvaluationContext(null, true);
        }

        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview must be in progress before it can be completed");
        }

        if (session.getQuestions().size() != QUESTION_COUNT) {
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
        AiInterviewSession session = resolveMySessionWithResult(sessionId);
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
        });

        AiInterviewFeedback feedback = feedbackRepository.findBySession_Id(sessionId)
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

    private void validateGeneratedQuestions(GeneratedQuestionSet generatedQuestionSet) {
        if (generatedQuestionSet == null || generatedQuestionSet.questions() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI did not return questions");
        }

        if (generatedQuestionSet.questions().size() != QUESTION_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI did not return exactly 7 questions");
        }

        Set<Integer> orders = new HashSet<>();
        for (GeneratedQuestion question : generatedQuestionSet.questions()) {
            if (question == null
                    || question.order() == null
                    || !orders.add(question.order())
                    || question.type() == null
                    || normalizeBlankToNull(question.question()) == null
                    || normalizeBlankToNull(question.rubric()) == null
                    || question.maxScore() == null
                    || question.maxScore() != 10) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned invalid questions");
            }
        }
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

    private record GenerationContext(
            Long sessionId,
            String jobTitle,
            String jobDescription,
            String experienceLevel,
            List<String> requiredSkills
    ) {
    }

    private record EvaluationContext(
            InterviewEvaluationRequest request,
            boolean alreadyCompleted
    ) {
    }
}
