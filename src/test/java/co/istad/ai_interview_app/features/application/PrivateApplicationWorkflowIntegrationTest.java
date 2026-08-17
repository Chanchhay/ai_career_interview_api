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
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(content().string(not(containsString("Rubric"))))
                .andExpect(content().string(not(containsString("test-model"))));

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/forward", applicationId)
                        .with(jwtFor("moderator-workflow", "MODERATOR")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/moderator/candidate-applications/{applicationId}/approve", applicationId)
                        .with(jwtFor("moderator-workflow", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"ready\"}"))
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

    private Long findApplicationId(Long jobId, Long seekerProfileId) {
        return transactionTemplate.execute(status -> entityManager
                .createQuery("""
                        select a.id
                        from JobApplication a
                        where a.jobPost.id = :jobId
                          and a.jobSeekerProfile.id = :seekerProfileId
                        """, Long.class)
                .setParameter("jobId", jobId)
                .setParameter("seekerProfileId", seekerProfileId)
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
            return (jobTitle, jobDescription, experienceLevel, requiredSkills) -> new GeneratedQuestionSet(List.of(
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
                                    "Good answer for question " + answer.order()
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
