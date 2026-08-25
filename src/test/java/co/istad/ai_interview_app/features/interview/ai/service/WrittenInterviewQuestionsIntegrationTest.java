package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetResponse;
import co.istad.ai_interview_app.features.interview.question.service.JobInterviewQuestionService;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a candidate is actually asked when an administrator has written
 * questions for the job.
 *
 * <p>Reuses the fake generator from {@link AiInterviewServiceImplTest}, which
 * answers whatever shape the config asks for — so a top-up that requested the
 * wrong number of questions fails here rather than passing against a fake that
 * always returns the same seven.
 *
 * <p>The platform default is 7 questions per interview; every expectation below
 * is written against that.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(AiInterviewServiceImplTest.FakeAiConfiguration.class)
class WrittenInterviewQuestionsIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final int TARGET_COUNT = 7;

    @Autowired
    private AiInterviewService aiInterviewService;

    @Autowired
    private JobInterviewQuestionService questionService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String seekerKeycloakId;

    @BeforeEach
    void setUpSecurity() {
        seekerKeycloakId = "written-seeker-" + SEQUENCE.incrementAndGet();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(seekerKeycloakId)
                .claim("realm_access", Map.of("roles", List.of("JOB_SEEKER")))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_JOB_SEEKER"))
        ));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void writtenQuestionsComeFirstAndTheAiFillsTheRest() {
        Long jobId = seedJob();
        write(jobId, ManualQuestionMode.MANUAL_PLUS_AI, "Explain the virtual DOM.", "Describe a conflict you resolved.");

        AiInterviewSessionResponse session = aiInterviewService.createInterviewForJob(jobId);

        assertThat(session.status()).isEqualTo(InterviewStatus.READY);
        assertThat(session.questions()).hasSize(TARGET_COUNT);
        assertThat(session.questions().get(0).questionText()).isEqualTo("Explain the virtual DOM.");
        assertThat(session.questions().get(1).questionText()).isEqualTo("Describe a conflict you resolved.");

        // Contiguous from 1, whichever half a question came from.
        assertThat(session.questions().stream().map(question -> question.displayOrder()).toList())
                .containsExactly(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    void manualOnlyAsksExactlyWhatWasWritten() {
        Long jobId = seedJob();
        write(jobId, ManualQuestionMode.MANUAL_ONLY, "Why this company?", "Walk me through a recent project.");

        AiInterviewSessionResponse session = aiInterviewService.createInterviewForJob(jobId);

        assertThat(session.questions()).hasSize(2);
        assertThat(session.questions().stream().map(question -> question.questionText()).toList())
                .containsExactly("Why this company?", "Walk me through a recent project.");
    }

    /**
     * More written questions than the interview is configured to hold. Nothing
     * is generated and nothing is dropped — the author's set wins, because
     * silently cutting somebody's question is worse than a longer interview.
     */
    @Test
    void aWrittenSetLongerThanTheTargetIsAskedInFull() {
        Long jobId = seedJob();
        String[] questions = new String[TARGET_COUNT + 2];
        for (int index = 0; index < questions.length; index++) {
            questions[index] = "Written question " + (index + 1);
        }

        write(jobId, ManualQuestionMode.MANUAL_PLUS_AI, questions);

        AiInterviewSessionResponse session = aiInterviewService.createInterviewForJob(jobId);

        assertThat(session.questions()).hasSize(TARGET_COUNT + 2);
        assertThat(session.questions().get(TARGET_COUNT + 1).questionText())
                .isEqualTo("Written question " + (TARGET_COUNT + 2));
    }

    /** A job nobody wrote questions for is generated exactly as it always was. */
    @Test
    void aJobWithNoWrittenQuestionsIsFullyGenerated() {
        Long jobId = seedJob();

        AiInterviewSessionResponse session = aiInterviewService.createInterviewForJob(jobId);

        assertThat(session.questions()).hasSize(TARGET_COUNT);
        assertThat(session.questions())
                .noneSatisfy(question ->
                        assertThat(question.questionText()).startsWith("Written question"));
    }

    @Test
    void savingAgainUpdatesInPlaceRatherThanPilingUp() {
        Long jobId = seedJob();
        JobInterviewQuestionSetResponse first =
                write(jobId, ManualQuestionMode.MANUAL_PLUS_AI, "First wording.", "Second question.");

        Long keptId = first.questions().get(0).id();

        JobInterviewQuestionSetResponse second = questionService.saveSet(
                jobId,
                new JobInterviewQuestionSetRequest(
                        ManualQuestionMode.MANUAL_ONLY,
                        List.of(new JobInterviewQuestionRequest(
                                keptId,
                                "Reworded.",
                                InterviewQuestionType.TECHNICAL,
                                "Mentions the tradeoffs.",
                                20
                        ))
                )
        );

        // The dropped question is gone, the kept one holds its id and its edits.
        assertThat(second.questions()).hasSize(1);
        assertThat(second.questions().get(0).id()).isEqualTo(keptId);
        assertThat(second.questions().get(0).questionText()).isEqualTo("Reworded.");
        assertThat(second.questions().get(0).maxScore()).isEqualTo(20);
        assertThat(second.mode()).isEqualTo(ManualQuestionMode.MANUAL_ONLY);
        assertThat(second.generatedQuestionCount()).isZero();
    }

    /** The count the editor shows before saving must match what really happens. */
    @Test
    void theSetReportsHowManyQuestionsTheAiWouldAdd()  {
        Long jobId = seedJob();

        assertThat(questionService.getSet(jobId).generatedQuestionCount()).isEqualTo(TARGET_COUNT);

        JobInterviewQuestionSetResponse topUp =
                write(jobId, ManualQuestionMode.MANUAL_PLUS_AI, "One.", "Two.");
        assertThat(topUp.generatedQuestionCount()).isEqualTo(TARGET_COUNT - 2);
    }

    /* ------------------------------------------------------------ seed --- */

    private JobInterviewQuestionSetResponse write(
            Long jobId,
            ManualQuestionMode mode,
            String... questionTexts
    ) {
        List<JobInterviewQuestionRequest> questions = java.util.Arrays.stream(questionTexts)
                .map(text -> new JobInterviewQuestionRequest(
                        null,
                        text,
                        InterviewQuestionType.TECHNICAL,
                        "A good answer explains why.",
                        null
                ))
                .toList();

        return questionService.saveSet(jobId, new JobInterviewQuestionSetRequest(mode, questions));
    }

    private Long seedJob() {
        return transactionTemplate.execute(status -> {
            int suffix = SEQUENCE.incrementAndGet();

            UserAccount seekerUser = new UserAccount();
            seekerUser.setKeycloakUserId(seekerKeycloakId);
            entityManager.persist(seekerUser);

            JobSeekerProfile seekerProfile = new JobSeekerProfile();
            seekerProfile.setUserAccount(seekerUser);
            entityManager.persist(seekerProfile);

            UserAccount recruiterUser = new UserAccount();
            recruiterUser.setKeycloakUserId("written-recruiter-" + suffix);
            entityManager.persist(recruiterUser);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(recruiterUser);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Written Questions Co " + suffix);
            entityManager.persist(company);

            JobPost jobPost = new JobPost();
            jobPost.setCompany(company);
            jobPost.setRecruiterProfile(recruiterProfile);
            jobPost.setTitle("Frontend Developer " + suffix);
            jobPost.setDescription("Build the candidate-facing app.");
            jobPost.setExperienceLevel("Junior");
            jobPost.setStatus(JobStatus.PUBLISHED);
            jobPost.setPublishedAt(Instant.now());
            entityManager.persist(jobPost);
            entityManager.flush();

            return jobPost.getId();
        });
    }
}
