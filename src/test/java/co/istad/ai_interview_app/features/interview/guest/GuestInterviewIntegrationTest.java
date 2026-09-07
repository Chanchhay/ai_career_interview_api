package co.istad.ai_interview_app.features.interview.guest;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewServiceImplTest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.service.JobInterviewQuestionService;
import co.istad.ai_interview_app.features.interview.vapi.dto.TranscriptSegmentationResult;
import co.istad.ai_interview_app.features.interview.vapi.service.AiInterviewTranscriptSegmenter;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;

/**
 * Interviews taken by someone with no account.
 *
 * <p>The risks worth testing are not "does it run" but "what else can it
 * reach": a guest must not read a stranger's interview, must not exceed the
 * limit an administrator set, and must not land in the hiring pipeline.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        AiInterviewServiceImplTest.FakeAiConfiguration.class,
        GuestInterviewIntegrationTest.FakeSegmenterConfiguration.class
})
class GuestInterviewIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String TOKEN_HEADER = "X-Guest-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JobInterviewQuestionService writtenQuestionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void enableGuestInterviews() throws Exception {
        settings(true, 2, 100, "FOLLOW_JOB");
    }

    @Test
    void aGuestCanSitAnInterviewAndSeeTheFullResult() throws Exception {
        UUID jobId = seedJob();

        JsonNode started = start(jobId, null);
        String token = started.get("guestToken").asText();
        UUID sessionId = UUID.fromString(started.get("session").get("id").asText());

        assertThat(token).isNotBlank();
        assertThat(started.get("attemptsUsed").asInt()).isEqualTo(1);
        assertThat(started.get("session").get("questions")).isNotEmpty();

        mockMvc.perform(post("/api/v1/public/guest-interviews/{id}/start", sessionId)
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        for (JsonNode question : started.get("session").get("questions")) {
            mockMvc.perform(put("/api/v1/public/guest-interviews/{id}/questions/{q}/answer",
                            sessionId, question.get("id").asText())
                            .header(TOKEN_HEADER, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answerText\":\"A considered answer.\"}"))
                    .andExpect(status().isOk());
        }

        // The same evaluation a signed-in candidate gets: score, per-answer
        // feedback and model answers, not a teaser.
        mockMvc.perform(post("/api/v1/public/guest-interviews/{id}/complete", sessionId)
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.feedback.overallScore").exists())
                .andExpect(jsonPath("$.data.session.questions[0].answer.feedback").isNotEmpty())
                .andExpect(jsonPath("$.data.session.questions[0].answer.modelAnswer").isNotEmpty());
    }

    /** The token is the only authorisation, so another token must not do. */
    @Test
    void anotherGuestsTokenCannotReadTheInterview() throws Exception {
        UUID jobId = seedJob();
        JsonNode started = start(jobId, null);
        UUID sessionId = UUID.fromString(started.get("session").get("id").asText());

        mockMvc.perform(get("/api/v1/public/guest-interviews/{id}", sessionId)
                        .header(TOKEN_HEADER, "someone-elses-token"))
                .andExpect(status().isNotFound());

        // And no token at all is not a way in either.
        mockMvc.perform(get("/api/v1/public/guest-interviews/{id}", sessionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void theAttemptLimitIsWhateverTheAdministratorSet() throws Exception {
        settings(true, 1, 100, "FOLLOW_JOB");
        UUID jobId = seedJob();

        JsonNode started = start(jobId, null);
        String token = started.get("guestToken").asText();

        mockMvc.perform(post("/api/v1/public/guest-interviews/jobs/{jobId}", jobId)
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/v1/public/guest-interviews/availability")
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canStart").value(false))
                .andExpect(jsonPath("$.data.attemptsUsed").value(1))
                .andExpect(jsonPath("$.data.attemptsAllowed").value(1));
    }

    @Test
    void guestInterviewsAreRefusedWhileTheyAreSwitchedOff() throws Exception {
        settings(false, 5, 100, "FOLLOW_JOB");
        UUID jobId = seedJob();

        mockMvc.perform(post("/api/v1/public/guest-interviews/jobs/{jobId}", jobId))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/public/guest-interviews/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.canStart").value(false));
    }

    /**
     * WRITTEN_ONLY asks exactly what an administrator wrote, whatever the job
     * itself is set to — the setting is a platform-wide choice about guests.
     */
    @Test
    void theAdministratorChoosesWhereGuestQuestionsComeFrom() throws Exception {
        UUID jobId = seedJob();
        writtenQuestionService.saveSet(jobId, new JobInterviewQuestionSetRequest(
                ManualQuestionMode.MANUAL_PLUS_AI,
                List.of(
                        new JobInterviewQuestionRequest(null, "Written one.", InterviewQuestionType.TECHNICAL, null, null),
                        new JobInterviewQuestionRequest(null, "Written two.", InterviewQuestionType.BEHAVIORAL, null, null)
                )
        ));

        settings(true, 5, 100, "WRITTEN_ONLY");

        JsonNode started = start(jobId, null);
        JsonNode questions = started.get("session").get("questions");

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).get("questionText").asText()).isEqualTo("Written one.");
    }

    /** A guest interview belongs to nobody, so nothing in hiring may see it. */
    @Test
    void aGuestInterviewNeverJoinsTheHiringPipeline() throws Exception {
        UUID jobId = seedJob();
        JsonNode started = start(jobId, null);
        UUID sessionId = UUID.fromString(started.get("session").get("id").asText());

        transactionTemplate.executeWithoutResult(status -> {
            /*
             * Loaded whole rather than selected field by field: a JPQL path
             * through a null association inner-joins it away, so
             * "select s.jobSeeker" on a guest session returns no row at all.
             */
            AiInterviewSession session = entityManager.find(AiInterviewSession.class, sessionId);

            assertThat(session).isNotNull();
            assertThat(session.getJobSeeker()).as("guest sessions have no owning account").isNull();
            assertThat(session.getApplication()).as("guest sessions have no application").isNull();
            assertThat(session.getGuestToken()).isNotBlank();
        });
    }

    /**
     * A guest can speak the interview instead of typing it, and is scored the
     * same way.
     *
     * <p>The voice path reaches the same session through a different door — a
     * call id bound to it, and a transcript posted when the call ends — so the
     * thing worth proving is that the guest's token still guards that door.
     */
    @Test
    void aGuestCanSitTheInterviewByVoice() throws Exception {
        UUID jobId = seedJob();

        JsonNode started = start(jobId, null);
        String token = started.get("guestToken").asText();
        UUID sessionId = UUID.fromString(started.get("session").get("id").asText());

        mockMvc.perform(post("/api/v1/public/guest-interviews/{id}/start", sessionId)
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk());

        String callId = "vapi-call-" + SEQUENCE.incrementAndGet();

        // Someone else's token cannot attach a call to this interview.
        mockMvc.perform(put("/api/v1/public/guest-interviews/{id}/vapi-call", sessionId)
                        .header(TOKEN_HEADER, "not-this-guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"callId\":\"%s\"}".formatted(callId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/public/guest-interviews/{id}/vapi-call", sessionId)
                        .header(TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"callId\":\"%s\"}".formatted(callId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/public/guest-interviews/{id}/transcript", sessionId)
                        .header(TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"turns":[
                                  {"role":"interviewer","text":"Tell me about your experience."},
                                  {"role":"candidate","text":"I have built several production services."}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Scored from the spoken answers, exactly as a typed interview is.
        mockMvc.perform(get("/api/v1/public/guest-interviews/{id}/result", sessionId)
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedback.overallScore").exists())
                .andExpect(jsonPath("$.data.session.questions[0].answer.answerText").isNotEmpty());
    }

    /**
     * Answers every question with what the candidate said, so the voice path can
     * be tested without calling out to Gemini to split a transcript.
     */
    @TestConfiguration
    static class FakeSegmenterConfiguration {

        @Bean
        @Primary
        AiInterviewTranscriptSegmenter fakeSegmenter() {
            return request -> new TranscriptSegmentationResult(
                    request.questions()
                            .stream()
                            .map(question -> new TranscriptSegmentationResult.SegmentedAnswer(
                                    question.questionId(),
                                    "Spoken answer for question " + question.displayOrder(),
                                    true
                            ))
                            .toList()
            );
        }
    }

    /* ------------------------------------------------------------ seed --- */

    private JsonNode start(UUID jobId, String token) throws Exception {
        var request = post("/api/v1/public/guest-interviews/jobs/{jobId}", jobId);
        if (token != null) request = request.header(TOKEN_HEADER, token);

        String body = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("data");
    }

    private void settings(boolean enabled, int perGuest, int perIp, String source) throws Exception {
        mockMvc.perform(put("/api/v1/admin/guest-interview-settings")
                        .with(moderatorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":%s,"maxAttemptsPerGuest":%d,"maxAttemptsPerIpPerDay":%d,"questionSource":"%s"}
                                """.formatted(enabled, perGuest, perIp, source)))
                .andExpect(status().isOk());
    }

    private static RequestPostProcessor moderatorJwt() {
        return jwt()
                .jwt(token -> token
                        .subject("guest-settings-moderator")
                        .claim("realm_access", Map.of("roles", List.of("MODERATOR"))))
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }

    private UUID seedJob() {
        return transactionTemplate.execute(status -> {
            int suffix = SEQUENCE.incrementAndGet();

            UserAccount recruiterUser = new UserAccount();
            recruiterUser.setKeycloakUserId("guest-recruiter-" + suffix);
            entityManager.persist(recruiterUser);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(recruiterUser);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Guest Interview Co " + suffix);
            company.setVerificationStatus(VerificationStatus.APPROVED);
            company.setStatus(ProfileStatus.ACTIVE);
            entityManager.persist(company);

            JobPost jobPost = new JobPost();
            jobPost.setCompany(company);
            jobPost.setRecruiterProfile(recruiterProfile);
            jobPost.setTitle("Guest Practice Role " + suffix);
            jobPost.setDescription("A role a visitor can practise against.");
            jobPost.setExperienceLevel("Junior");
            jobPost.setStatus(JobStatus.PUBLISHED);
            jobPost.setPublishedAt(Instant.now());
            entityManager.persist(jobPost);
            entityManager.flush();

            return jobPost.getId();
        });
    }
}
