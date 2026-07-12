package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.EvaluatedAnswer;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestion;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestionSet;
import co.istad.ai_interview_app.features.interview.ai.dto.InterviewEvaluationResult;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewSessionRepository;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.JobPostSection;
import co.istad.ai_interview_app.features.job.entity.JobPostSkill;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import co.istad.ai_interview_app.shared.enums.job.JobPostSectionType;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AiInterviewServiceImplTest {

    private static final String SEEKER_KEYCLOAK_ID = "test-seeker";

    @Autowired
    private AiInterviewService aiInterviewService;

    @Autowired
    private AiInterviewSessionRepository sessionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUpSecurity() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(SEEKER_KEYCLOAK_ID)
                .claim("realm_access", java.util.Map.of("roles", List.of("JOB_SEEKER")))
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
    void completesTextInterviewWorkflowWithFakeAi() {
        Long jobId = createPublishedJobAndSeeker();

        AiInterviewSessionResponse created = aiInterviewService.createInterviewForJob(jobId);

        assertThat(created.status()).isEqualTo(InterviewStatus.READY);
        assertThat(created.questionCount()).isEqualTo(7);
        assertThat(created.answeredCount()).isZero();
        assertThat(created.questions()).hasSize(7);
        assertThat(created.questions())
                .allSatisfy(question -> {
                    assertThat(question.id()).isNotNull();
                    assertThat(question.questionText()).isNotBlank();
                    assertThat(question.maxScore()).isEqualTo(10);
                    assertThat(question.answer()).isNull();
                });

        AiInterviewSessionResponse started = aiInterviewService.startInterview(created.id());
        assertThat(started.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(started.startedAt()).isNotNull();

        for (var question : started.questions()) {
            aiInterviewService.submitAnswer(
                    started.id(),
                    question.id(),
                    new AiInterviewAnswerRequest("My answer for question " + question.displayOrder())
            );
        }

        AiInterviewResultResponse completed = aiInterviewService.completeInterview(started.id());

        assertThat(completed.session().status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(completed.session().totalScore()).isEqualByComparingTo("8.00");
        assertThat(completed.feedback().result()).isEqualTo(InterviewResult.PASSED);
        assertThat(completed.questions())
                .allSatisfy(question -> {
                    assertThat(question.answer()).isNotNull();
                    assertThat(question.answer().score()).isEqualByComparingTo("8.00");
                    assertThat(question.answer().feedback()).isNotBlank();
                });

        AiInterviewResultResponse completedAgain = aiInterviewService.completeInterview(started.id());
        assertThat(completedAgain.session().status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(Duration.between(completedAgain.session().endedAt(), completed.session().endedAt()).abs())
                .isLessThan(Duration.ofMillis(1));

        AiInterviewSession session = sessionRepository.findById(created.id()).orElseThrow();
        assertThat(session.getApplication()).isNull();
    }

    Long createPublishedJobAndSeeker() {
        return transactionTemplate.execute(status -> {
            UserAccount seekerUser = new UserAccount();
            seekerUser.setKeycloakUserId(SEEKER_KEYCLOAK_ID);
            entityManager.persist(seekerUser);

            JobSeekerProfile jobSeekerProfile = new JobSeekerProfile();
            jobSeekerProfile.setUserAccount(seekerUser);
            entityManager.persist(jobSeekerProfile);

            UserAccount recruiterUser = new UserAccount();
            recruiterUser.setKeycloakUserId("test-recruiter");
            entityManager.persist(recruiterUser);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(recruiterUser);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Test Company");
            entityManager.persist(company);

            Skill java = new Skill();
            java.setName("Java");
            java.setSkillType("TECHNICAL");
            entityManager.persist(java);

            JobPost jobPost = new JobPost();
            jobPost.setCompany(company);
            jobPost.setRecruiterProfile(recruiterProfile);
            jobPost.setTitle("Backend Java Developer");
            jobPost.setDescription("Build Spring Boot APIs for the AI interview platform.");
            jobPost.setExperienceLevel("Junior");
            jobPost.setStatus(JobStatus.PUBLISHED);
            jobPost.setPublishedAt(Instant.now());

            JobPostSection section = new JobPostSection();
            section.setJobPost(jobPost);
            section.setSectionType(JobPostSectionType.ABOUT_ROLE);
            section.setTitle("About the role");
            section.setContentMarkdown("You will work on backend APIs.");
            section.setContentText("You will work on backend APIs.");
            section.setDisplayOrder(1);
            jobPost.getSections().add(section);

            JobPostSkill skill = new JobPostSkill();
            skill.setJobPost(jobPost);
            skill.setSkill(java);
            skill.setRequiredLevel("Junior");
            jobPost.getSkills().add(skill);

            entityManager.persist(jobPost);
            entityManager.flush();

            return jobPost.getId();
        });
    }

    @TestConfiguration
    static class FakeAiConfiguration {

        @Bean
        @Primary
        AiInterviewQuestionGenerator fakeQuestionGenerator() {
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
        AiInterviewEvaluator fakeEvaluator() {
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
                    "Could add more production examples.",
                    "Proceed to next interview round.",
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
