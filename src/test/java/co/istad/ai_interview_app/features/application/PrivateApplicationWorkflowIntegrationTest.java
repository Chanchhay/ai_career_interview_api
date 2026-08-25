package co.istad.ai_interview_app.features.application;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.EvaluatedAnswer;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestion;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestionSet;
import co.istad.ai_interview_app.features.interview.ai.dto.InterviewEvaluationResult;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewEvaluator;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewQuestionGenerator;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewService;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivateApplicationWorkflowIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AiInterviewService aiInterviewService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void applicationCreationValidatesDuplicatesStatusExpirationResumeOwnershipAndAccess() throws Exception {
        Fixture fixture = createFixture("apply-validations", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        Fixture other = createFixture("apply-other", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        Long draftJobId = transactionTemplate.execute(status ->
                createJob(fixture.ownerRecruiterProfileId, fixture.companyId, "Draft Job", JobStatus.DRAFT, null));
        Long expiredJobId = transactionTemplate.execute(status ->
                createJob(fixture.ownerRecruiterProfileId, fixture.companyId, "Expired Job", JobStatus.PUBLISHED, Instant.now().minus(1, ChronoUnit.DAYS)));

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d,"coverLetter":"private-cover-letter"}
                                """.formatted(fixture.privateResumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value(fixture.privateResumeId))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", draftJobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", expiredJobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", other.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d}
                                """.formatted(other.privateResumeId)))
                .andExpect(status().isNotFound());

        Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);
        mockMvc.perform(get("/api/v1/job-seeker/applications/{applicationId}", applicationId)
                        .with(jwtFor(other.seekerKeycloakId, "SEEKER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void privateApplicationAiModeratorForwardingAndRecruiterVisibilityAreGated() throws Exception {
        Fixture fixture = createFixture("workflow", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        Fixture otherRecruiterFixture = createFixture("workflow-other-recruiter", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        createModerator("moderator-workflow");

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d,"coverLetter":"forwarding-secret-cover-letter"}
                                """.formatted(fixture.privateResumeId)))
                .andExpect(status().isOk());

        Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);

        mockMvc.perform(get("/api/v1/recruiter/forwarded-applications/{applicationId}", applicationId)
                        .with(jwtFor(fixture.ownerRecruiterKeycloakId, "RECRUITER")))
                .andExpect(status().isNotFound());

        completeApplicationAiInterview(fixture.seekerKeycloakId, applicationId);

        transactionTemplate.executeWithoutResult(status -> {
            JobApplication application = entityManager.find(JobApplication.class, applicationId);
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.MODERATOR_REVIEW_PENDING);
        });

        mockMvc.perform(get("/api/v1/moderator/candidate-applications")
                        .param("status", "PENDING")
                        .with(jwtFor("moderator-workflow", "MODERATOR")))
                .andExpect(status().isOk())
                /*
                 * At least one, not exactly one. These tests share a database
                 * with no rollback between them, and every submitted
                 * application now opens a review row — so a global count here
                 * would assert on whatever else has run first rather than on
                 * anything this test did.
                 */
                .andExpect(jsonPath("$.data.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(content().string(not(containsString("Rubric"))))
                .andExpect(content().string(not(containsString("test-model"))));

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/forward", applicationId)
                        .with(jwtFor("moderator-workflow", "MODERATOR")))
                .andExpect(status().isBadRequest());

        String scheduleResponse = mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/human-interviews", applicationId)
                        .with(jwtFor("moderator-workflow", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scheduledAt":"%s","meetingUrl":"https://meet.example/%d"}
                                """.formatted(Instant.now().plus(1, ChronoUnit.DAYS), applicationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long interviewId = extractId(scheduleResponse);

        // Booking an interview and then approving before holding it contradicts
        // the act of booking, so approval is refused for as long as it is
        // outstanding. Completing or cancelling settles it.
        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/approve", applicationId)
                        .with(jwtFor("moderator-workflow", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"ready\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/moderator/human-interviews/{interviewId}/complete", interviewId)
                        .with(jwtFor("moderator-workflow", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PASSED\",\"note\":\"human interview passed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/approve", applicationId)
                        .with(jwtFor("moderator-workflow", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"internal moderator note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"));

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/forward", applicationId)
                        .with(jwtFor("moderator-workflow", "MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("FORWARDED"));

        mockMvc.perform(get("/api/v1/recruiter/forwarded-applications/{applicationId}", applicationId)
                        .with(jwtFor(otherRecruiterFixture.ownerRecruiterKeycloakId, "RECRUITER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/recruiter/forwarded-applications/{applicationId}", applicationId)
                        .with(jwtFor(fixture.ownerRecruiterKeycloakId, "RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.application.id").value(applicationId))
                .andExpect(content().string(containsString("forwarding-secret-cover-letter")))
                .andExpect(content().string(not(containsString("internal moderator note"))))
                .andExpect(content().string(not(containsString("Rubric"))))
                .andExpect(content().string(not(containsString("test-model"))));
    }

    /**
     * The ordinary path: the candidate passed the AI interview and nobody asked
     * them to a human one.
     *
     * <p>This used to be impossible. Approval demanded a completed human
     * interview unconditionally, and nothing schedules one automatically, so a
     * moderator who wanted to approve on the AI result alone had no way to do
     * it — the only escape was to book an interview purely to close it again.
     */
    @Test
    void approvesOnTheAiInterviewAloneWhenNoHumanInterviewWasScheduled() throws Exception {
        Fixture fixture = createFixture("ai-only", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        createModerator("moderator-ai-only");

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d}
                                """.formatted(fixture.privateResumeId)))
                .andExpect(status().isOk());

        Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);
        completeApplicationAiInterview(fixture.seekerKeycloakId, applicationId);

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/approve", applicationId)
                        .with(jwtFor("moderator-ai-only", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"AI result is enough\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"));
    }

    /** A cancelled interview is settled, so it must stop blocking the decision. */
    @Test
    void approvesAfterAScheduledHumanInterviewIsCancelled() throws Exception {
        Fixture fixture = createFixture("cancelled-hi", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        createModerator("moderator-cancelled-hi");

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d}
                                """.formatted(fixture.privateResumeId)))
                .andExpect(status().isOk());

        Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);
        completeApplicationAiInterview(fixture.seekerKeycloakId, applicationId);

        String scheduled = mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/human-interviews", applicationId)
                        .with(jwtFor("moderator-cancelled-hi", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scheduledAt":"%s","meetingUrl":"https://meet.example/cancelled"}
                                """.formatted(Instant.now().plus(1, ChronoUnit.DAYS))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/approve", applicationId)
                        .with(jwtFor("moderator-cancelled-hi", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"too early\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/moderator/human-interviews/{interviewId}/cancel", extractId(scheduled))
                        .with(jwtFor("moderator-cancelled-hi", "MODERATOR")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/approve", applicationId)
                        .with(jwtFor("moderator-cancelled-hi", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"went ahead without it\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"));
    }

    /**
     * A candidate the AI marked FAILED cannot be approved.
     *
     * <p>Approval used to check only that a session had finished, so a failing
     * score was recorded and then ignored by the one decision it should have
     * informed.
     *
     * <p>NEEDS_REVIEW is deliberately not covered here as a blocker: it means
     * the machine could not decide, which is precisely when a moderator should
     * be able to.
     */
    @Test
    void refusesApprovalWhenTheAiInterviewFailed() throws Exception {
        Fixture fixture = createFixture("ai-failed", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        createModerator("moderator-ai-failed");

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d}
                                """.formatted(fixture.privateResumeId)))
                .andExpect(status().isOk());

        Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);
        completeApplicationAiInterview(fixture.seekerKeycloakId, applicationId);

        // The stubbed evaluator does not fail anyone, so the verdict is forced
        // here rather than coaxed out of it.
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createQuery("""
                        update AiInterviewSession session
                        set session.result = :result
                        where session.application.id = :applicationId
                        """)
                .setParameter("result", InterviewResult.FAILED)
                .setParameter("applicationId", applicationId)
                .executeUpdate());

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/approve", applicationId)
                        .with(jwtFor("moderator-ai-failed", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"overriding the machine\"}"))
                .andExpect(status().isBadRequest());

        // Rejecting stays available: a failed candidate still needs closing out.
        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/reject", applicationId)
                        .with(jwtFor("moderator-ai-failed", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"did not pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("REJECTED"));
    }

    /**
     * The candidate sat the interview from the job page rather than from their
     * application, which is what the public "Start AI interview" button does.
     *
     * <p>That session used to be created with a null application, so it was
     * invisible to the approval check and the moderator was told no AI
     * interview existed — for a candidate who had just completed one.
     */
    @Test
    void approvesWhenTheAiInterviewWasStartedFromTheJobRatherThanTheApplication() throws Exception {
        Fixture fixture = createFixture("job-started-ai", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        createModerator("moderator-job-started");

        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d}
                                """.formatted(fixture.privateResumeId)))
                .andExpect(status().isOk());

        Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);

        // The job route, not the application route.
        setSecurity(fixture.seekerKeycloakId, "SEEKER");
        AiInterviewSessionResponse session = aiInterviewService.createInterviewForJob(fixture.jobId);
        AiInterviewSessionResponse started = aiInterviewService.startInterview(session.id());
        for (var question : started.questions()) {
            aiInterviewService.submitAnswer(
                    session.id(),
                    question.id(),
                    new AiInterviewAnswerRequest("Answer " + question.displayOrder())
            );
        }
        aiInterviewService.completeInterview(session.id());
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/approve", applicationId)
                        .with(jwtFor("moderator-job-started", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"interview counted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"));
    }

    /**
     * A rejected candidate can apply to the same job again, and the rejection
     * stays on record.
     *
     * <p>Applying used to be permanently one-shot per job: a plain unique
     * constraint on (job, candidate) meant a rejection closed the door for
     * good, even after the recruiter reposted the role. The rule is now one
     * *live* application at a time.
     */
    @Test
    void allowsReapplyingAfterRejectionWhileKeepingTheClosedAttempt() throws Exception {
        Fixture fixture = createFixture("reapply", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        createModerator("moderator-reapply");

        applyTo(fixture);

        Long firstApplicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);

        // A second attempt while the first is still open is still refused.
        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d}
                                """.formatted(fixture.privateResumeId)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/reject", firstApplicationId)
                        .with(jwtFor("moderator-reapply", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"not this time\"}"))
                .andExpect(status().isOk());

        applyTo(fixture);

        // Two rows now: the rejection is kept rather than overwritten.
        transactionTemplate.executeWithoutResult(status -> {
            Long total = entityManager.createQuery("""
                            select count(application)
                            from JobApplication application
                            where application.jobPost.id = :jobId
                              and application.jobSeekerProfile.id = :profileId
                            """, Long.class)
                    .setParameter("jobId", fixture.jobId)
                    .setParameter("profileId", fixture.seekerProfileId)
                    .getSingleResult();

            assertThat(total).isEqualTo(2);
            assertThat(entityManager.find(JobApplication.class, firstApplicationId).getStatus())
                    .isEqualTo(ApplicationStatus.REJECTED);
        });
    }

    /** Withdrawing frees the slot the same way a rejection does. */
    @Test
    void allowsReapplyingAfterWithdrawing() throws Exception {
        Fixture fixture = createFixture("reapply-withdraw", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));

        applyTo(fixture);
        Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);

        mockMvc.perform(post("/api/v1/job-seeker/applications/{applicationId}/withdraw", applicationId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER")))
                .andExpect(status().isOk());

        applyTo(fixture);
    }

    /** Submits an application for the fixture's seeker and expects it to succeed. */
    /**
     * The re-apply cooldown, and that an administrator controls it.
     *
     * <p>Restores the setting in a finally block: it is a single global row, and
     * these tests share one database with no rollback, so leaving it raised
     * would silently fail whichever re-apply test happens to run next.
     */
    @Test
    void enforcesTheAdminConfiguredReapplyCooldown() throws Exception {
        Fixture fixture = createFixture("cooldown", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));
        createModerator("moderator-cooldown");

        try {
            setReapplyCooldownDays(7);

            applyTo(fixture);
            Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);

            mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/reject", applicationId)
                            .with(jwtFor("moderator-cooldown", "MODERATOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decisionNote\":\"not now\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                            .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"resumeId":%d}
                                    """.formatted(fixture.privateResumeId)))
                    .andExpect(status().isConflict());

            // Move the rejection back beyond the window rather than waiting.
            transactionTemplate.executeWithoutResult(status -> entityManager
                    .createQuery("""
                            update JobApplication application
                            set application.closedAt = :closedAt
                            where application.id = :applicationId
                            """)
                    .setParameter("closedAt", Instant.now().minus(8, ChronoUnit.DAYS))
                    .setParameter("applicationId", applicationId)
                    .executeUpdate());

            applyTo(fixture);
        } finally {
            setReapplyCooldownDays(0);
        }
    }

    /**
     * Withdrawing is the candidate's own decision, so it is not held back —
     * only a rejection starts the clock.
     */
    @Test
    void doesNotApplyTheCooldownToAWithdrawnApplication() throws Exception {
        Fixture fixture = createFixture("cooldown-withdraw", JobStatus.PUBLISHED, Instant.now().plus(5, ChronoUnit.DAYS));

        try {
            setReapplyCooldownDays(30);

            applyTo(fixture);
            Long applicationId = findApplicationId(fixture.jobId, fixture.seekerProfileId);

            mockMvc.perform(post("/api/v1/job-seeker/applications/{applicationId}/withdraw", applicationId)
                            .with(jwtFor(fixture.seekerKeycloakId, "SEEKER")))
                    .andExpect(status().isOk());

            applyTo(fixture);
        } finally {
            setReapplyCooldownDays(0);
        }
    }

    /**
     * No local moderator row is created here on purpose. The settings endpoint
     * only records who changed it when the account happens to exist, so a JWT
     * is enough — and creating one per call would collide with itself the
     * second time this helper runs.
     */
    private void setReapplyCooldownDays(int days) throws Exception {
        mockMvc.perform(put("/api/v1/admin/application-settings")
                        .with(jwtFor("admin-cooldown-setter", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reapplyCooldownDays\":%d}".formatted(days)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reapplyCooldownDays").value(days));
    }

    private void applyTo(Fixture fixture) throws Exception {
        mockMvc.perform(post("/api/v1/job-seeker/jobs/{jobId}/applications", fixture.jobId)
                        .with(jwtFor(fixture.seekerKeycloakId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":%d}
                                """.formatted(fixture.privateResumeId)))
                .andExpect(status().isOk());
    }

    private void completeApplicationAiInterview(String seekerKeycloakId, Long applicationId) {
        setSecurity(seekerKeycloakId, "SEEKER");

        AiInterviewSessionResponse created = aiInterviewService.createInterviewForApplication(applicationId);
        assertThat(created.applicationId()).isEqualTo(applicationId);
        assertThat(created.status()).isEqualTo(InterviewStatus.READY);

        AiInterviewSessionResponse started = aiInterviewService.startInterview(created.id());
        for (var question : started.questions()) {
            aiInterviewService.submitAnswer(
                    created.id(),
                    question.id(),
                    new AiInterviewAnswerRequest("Answer " + question.displayOrder())
            );
        }
        aiInterviewService.completeInterview(created.id());
        SecurityContextHolder.clearContext();
    }

    private Fixture createFixture(String marker, JobStatus jobStatus, Instant expiredAt) {
        return transactionTemplate.execute(status -> {
            int suffix = SEQUENCE.incrementAndGet();

            UserAccount seekerUser = new UserAccount();
            seekerUser.setKeycloakUserId("seeker-" + marker + "-" + suffix);
            entityManager.persist(seekerUser);

            JobSeekerProfile seekerProfile = new JobSeekerProfile();
            seekerProfile.setUserAccount(seekerUser);
            seekerProfile.setHeadline("Private Candidate " + marker);
            seekerProfile.setCurrentPosition("Backend Developer");
            entityManager.persist(seekerProfile);

            Resume privateResume = new Resume();
            privateResume.setJobSeekerProfile(seekerProfile);
            privateResume.setTitle("Private Resume " + marker);
            privateResume.setResumeFileUrl("https://files.example/private-" + suffix + ".pdf");
            privateResume.setVisibility(VisibilityStatus.PRIVATE);
            entityManager.persist(privateResume);

            UserAccount recruiterUser = new UserAccount();
            recruiterUser.setKeycloakUserId("recruiter-" + marker + "-" + suffix);
            entityManager.persist(recruiterUser);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(recruiterUser);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Company " + marker);
            entityManager.persist(company);

            Long jobId = createJob(recruiterProfile.getId(), company.getId(), "Job " + marker, jobStatus, expiredAt);

            entityManager.flush();

            return new Fixture(
                    seekerUser.getKeycloakUserId(),
                    seekerProfile.getId(),
                    privateResume.getId(),
                    recruiterUser.getKeycloakUserId(),
                    recruiterProfile.getId(),
                    company.getId(),
                    jobId
            );
        });
    }

    private Long createJob(Long recruiterProfileId, Long companyId, String title, JobStatus status, Instant expiredAt) {
        RecruiterProfile recruiterProfile = entityManager.find(RecruiterProfile.class, recruiterProfileId);
        Company company = entityManager.find(Company.class, companyId);
        JobPost jobPost = new JobPost();
        jobPost.setRecruiterProfile(recruiterProfile);
        jobPost.setCompany(company);
        jobPost.setTitle(title);
        jobPost.setDescription("Private workflow test job");
        jobPost.setStatus(status);
        jobPost.setExpiredAt(expiredAt);
        if (status == JobStatus.PUBLISHED) {
            jobPost.setPublishedAt(Instant.now());
        }
        entityManager.persist(jobPost);
        entityManager.flush();
        return jobPost.getId();
    }

    private void createModerator(String keycloakUserId) {
        transactionTemplate.executeWithoutResult(status -> {
            UserAccount moderatorUser = new UserAccount();
            moderatorUser.setKeycloakUserId(keycloakUserId);
            entityManager.persist(moderatorUser);

            ModeratorProfile moderatorProfile = new ModeratorProfile();
            moderatorProfile.setUserAccount(moderatorUser);
            entityManager.persist(moderatorProfile);
        });
    }

    /**
     * The candidate's current application for a job.
     *
     * <p>Newest first rather than a single result: a candidate may now hold
     * several attempts at one job once earlier ones are closed, and callers
     * always mean the one they just created.
     */
    private Long findApplicationId(Long jobId, Long seekerProfileId) {
        return transactionTemplate.execute(status -> entityManager
                .createQuery("""
                        select a.id
                        from JobApplication a
                        where a.jobPost.id = :jobId
                          and a.jobSeekerProfile.id = :seekerProfileId
                        order by a.id desc
                        """, Long.class)
                .setParameter("jobId", jobId)
                .setParameter("seekerProfileId", seekerProfileId)
                .setMaxResults(1)
                .getSingleResult());
    }

    private static Long extractId(String responseBody) {
        String marker = "\"id\":";
        int start = responseBody.indexOf(marker) + marker.length();
        int end = responseBody.indexOf(',', start);
        return Long.valueOf(responseBody.substring(start, end));
    }

    private static void setSecurity(String subject, String role) {
        Jwt jwt = Jwt.withTokenValue("test-token-" + subject)
                .header("alg", "none")
                .subject(subject)
                .claim("realm_access", java.util.Map.of("roles", List.of(role)))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        ));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(String subject, String role) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject(subject)
                        .claim("realm_access", java.util.Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private record Fixture(
            String seekerKeycloakId,
            Long seekerProfileId,
            Long privateResumeId,
            String ownerRecruiterKeycloakId,
            Long ownerRecruiterProfileId,
            Long companyId,
            Long jobId
    ) {
    }

    @TestConfiguration
    static class FakeAiConfiguration {

        @Bean
        @Primary
        AiInterviewQuestionGenerator fakeApplicationQuestionGenerator() {
            return (jobTitle, jobDescription, experienceLevel, requiredSkills, config) -> new GeneratedQuestionSet(List.of(
                    question(1, InterviewQuestionType.TECHNICAL),
                    question(2, InterviewQuestionType.TECHNICAL),
                    question(3, InterviewQuestionType.TECHNICAL),
                    question(4, InterviewQuestionType.TECHNICAL),
                    question(5, InterviewQuestionType.BEHAVIORAL),
                    question(6, InterviewQuestionType.BEHAVIORAL),
                    question(7, InterviewQuestionType.SITUATIONAL)
            ));
        }

        @Bean
        @Primary
        AiInterviewEvaluator fakeApplicationEvaluator() {
            return request -> new InterviewEvaluationResult(
                    request.answers()
                            .stream()
                            .map(answer -> new EvaluatedAnswer(
                                    answer.questionId(),
                                    new BigDecimal("8.00"),
                                    "Good answer for question " + answer.order(),
                                    "Model answer for question " + answer.order()
                            ))
                            .toList(),
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    "Clear technical foundation.",
                    "Could add more examples.",
                    "Proceed to moderator review.",
                    InterviewResult.PASSED
            );
        }

        private static GeneratedQuestion question(int order, InterviewQuestionType type) {
            return new GeneratedQuestion(
                    order,
                    type,
                    "Question " + order,
                    "Rubric " + order,
                    10
            );
        }
    }
}
