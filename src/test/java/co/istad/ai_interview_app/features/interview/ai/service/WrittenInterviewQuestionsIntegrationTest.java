package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetResponse;
import co.istad.ai_interview_app.features.interview.question.dto.PublicJobInterviewPreviewResponse;
import co.istad.ai_interview_app.features.interview.question.service.JobInterviewQuestionService;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.UUID;

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
        UUID jobId = seedJob();
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
        UUID jobId = seedJob();
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
        UUID jobId = seedJob();
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
        UUID jobId = seedJob();

        AiInterviewSessionResponse session = aiInterviewService.createInterviewForJob(jobId);

        assertThat(session.questions()).hasSize(TARGET_COUNT);
        assertThat(session.questions())
                .noneSatisfy(question ->
                        assertThat(question.questionText()).startsWith("Written question"));
    }

    @Test
    void savingAgainUpdatesInPlaceRatherThanPilingUp() {
        UUID jobId = seedJob();
        JobInterviewQuestionSetResponse first =
                write(jobId, ManualQuestionMode.MANUAL_PLUS_AI, "First wording.", "Second question.");

        UUID keptId = first.questions().get(0).id();

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
        UUID jobId = seedJob();

        assertThat(questionService.getSet(jobId).generatedQuestionCount()).isEqualTo(TARGET_COUNT);

        JobInterviewQuestionSetResponse topUp =
                write(jobId, ManualQuestionMode.MANUAL_PLUS_AI, "One.", "Two.");
        assertThat(topUp.generatedQuestionCount()).isEqualTo(TARGET_COUNT - 2);
    }

    /* ------------------------------------------------- public preview --- */

    /**
     * The preview counts the whole interview, not just the written half — a
     * visitor deciding whether to start needs the real length.
     */
    @Test
    void thePublicPreviewShowsWrittenQuestionsAndCountsTheGeneratedOnesToo() {
        UUID jobId = seedJob();
        write(jobId, ManualQuestionMode.MANUAL_PLUS_AI, "Explain the virtual DOM.", "Describe a conflict you resolved.");

        PublicJobInterviewPreviewResponse preview = questionService.getPublicPreview(jobId);

        assertThat(preview.questionCount()).isEqualTo(TARGET_COUNT);
        assertThat(preview.questions()).hasSize(2);
        assertThat(preview.questions().stream().map(question -> question.questionText()).toList())
                .containsExactly("Explain the virtual DOM.", "Describe a conflict you resolved.");
        assertThat(preview.estimatedMinutes()).isEqualTo(TARGET_COUNT * 2);
    }

    /**
     * The rubric is what an answer is marked against. Publishing it would hand
     * every candidate the mark scheme, so the public record has no field for it.
     */
    @Test
    void thePublicPreviewNeverCarriesTheRubric() {
        UUID jobId = seedJob();
        write(jobId, ManualQuestionMode.MANUAL_ONLY, "Why this company?");

        PublicJobInterviewPreviewResponse preview = questionService.getPublicPreview(jobId);

        assertThat(preview.questions().get(0))
                .hasNoNullFieldsOrProperties()
                .extracting(question -> question.getClass().getRecordComponents().length)
                .isEqualTo(5);
        assertThat(questionService.getSet(jobId).questions().get(0).expectedAnswer())
                .isEqualTo("A good answer explains why.");
    }

    /** MANUAL_ONLY shows the whole interview, so nothing is left to explain. */
    @Test
    void aManualOnlyPreviewListsEveryQuestionTheInterviewAsks() {
        UUID jobId = seedJob();
        write(jobId, ManualQuestionMode.MANUAL_ONLY, "One.", "Two.", "Three.");

        PublicJobInterviewPreviewResponse preview = questionService.getPublicPreview(jobId);

        assertThat(preview.questionCount()).isEqualTo(3);
        assertThat(preview.questions()).hasSize(3);
    }

    /**
     * A job nobody wrote for still has an interview — it is generated in full —
     * so the count stands while the list is empty.
     */
    @Test
    void aJobWithNoWrittenQuestionsPreviewsAnEmptyList() {
        UUID jobId = seedJob();

        PublicJobInterviewPreviewResponse preview = questionService.getPublicPreview(jobId);

        assertThat(preview.questions()).isEmpty();
        assertThat(preview.questionCount()).isEqualTo(TARGET_COUNT);
    }

    /** An unpublished job's questions are as private as the job itself. */
    @Test
    void anUnpublishedJobHasNoPublicPreview() {
        UUID jobId = seedJob();
        write(jobId, ManualQuestionMode.MANUAL_ONLY, "Secret screening question.");

        transactionTemplate.executeWithoutResult(status -> {
            JobPost jobPost = entityManager.find(JobPost.class, jobId);
            jobPost.setStatus(JobStatus.DRAFT);
        });

        assertThatThrownBy(() -> questionService.getPublicPreview(jobId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Public job was not found");
    }

    /**
     * A page of the board in one call. Order follows the ids the caller asked
     * with, because that is the order the cards are already in on screen.
     */
    @Test
    void thePublicBatchPreviewAnswersForAPageOfJobsInTheOrderAsked() {
        UUID first = seedJob();
        UUID second = seedJob();
        write(first, ManualQuestionMode.MANUAL_ONLY, "First job question.");
        write(second, ManualQuestionMode.MANUAL_ONLY, "Second job question.");

        List<PublicJobInterviewPreviewResponse> previews =
                questionService.getPublicPreviews(List.of(second, first));

        assertThat(previews).hasSize(2);
        assertThat(previews.get(0).jobId()).isEqualTo(second);
        assertThat(previews.get(1).jobId()).isEqualTo(first);
        assertThat(previews.get(0).questions().get(0).questionText()).isEqualTo("Second job question.");
    }

    /** A job that left the board takes its preview with it, quietly. */
    @Test
    void thePublicBatchPreviewDropsJobsThatAreNoLongerPublic() {
        UUID published = seedJob();
        UUID withdrawn = seedJob();
        write(published, ManualQuestionMode.MANUAL_ONLY, "Still hiring.");
        write(withdrawn, ManualQuestionMode.MANUAL_ONLY, "Not any more.");

        transactionTemplate.executeWithoutResult(status ->
                entityManager.find(JobPost.class, withdrawn).setStatus(JobStatus.DRAFT));

        List<PublicJobInterviewPreviewResponse> previews =
                questionService.getPublicPreviews(List.of(published, withdrawn));

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).jobId()).isEqualTo(published);
    }

    /** The single and batch routes must describe a job identically. */
    @Test
    void theBatchPreviewAgreesWithTheSingleOne() {
        UUID jobId = seedJob();
        write(jobId, ManualQuestionMode.MANUAL_PLUS_AI, "One.", "Two.");

        assertThat(questionService.getPublicPreviews(List.of(jobId)))
                .containsExactly(questionService.getPublicPreview(jobId));
    }

    @Test
    void anEmptyBatchAsksTheDatabaseNothing() {
        assertThat(questionService.getPublicPreviews(List.of())).isEmpty();
    }

    /* ------------------------------------------------------------ seed --- */

    private JobInterviewQuestionSetResponse write(
            UUID jobId,
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

    private UUID seedJob() {
        return transactionTemplate.execute(status -> {
            int suffix = SEQUENCE.incrementAndGet();

            /*
             * The seeker is per-test, not per-job, so a test that seeds two
             * jobs reuses the one it already made — persisting it twice would
             * collide on the unique keycloak id.
             */
            if (entityManager.createQuery(
                            "select count(account) from UserAccount account where account.keycloakUserId = :id",
                            Long.class)
                    .setParameter("id", seekerKeycloakId)
                    .getSingleResult() == 0L) {

                UserAccount seekerUser = new UserAccount();
                seekerUser.setKeycloakUserId(seekerKeycloakId);
                entityManager.persist(seekerUser);

                JobSeekerProfile seekerProfile = new JobSeekerProfile();
                seekerProfile.setUserAccount(seekerUser);
                entityManager.persist(seekerProfile);
            }

            UserAccount recruiterUser = new UserAccount();
            recruiterUser.setKeycloakUserId("written-recruiter-" + suffix);
            entityManager.persist(recruiterUser);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(recruiterUser);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Written Questions Co " + suffix);
            // The public job query requires both: a PUBLISHED job behind an
            // unapproved company is not on the board, so a fixture that left
            // these at their PENDING defaults would not be a public job at all.
            company.setVerificationStatus(VerificationStatus.APPROVED);
            company.setStatus(ProfileStatus.ACTIVE);
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
