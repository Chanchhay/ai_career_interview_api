package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.profile.SalaryVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobSeekerResourceManagementIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void seekerCanReadAndPatchProfileWithoutChangingProtectedPublicationFields() throws Exception {
        Fixture fixture = createSeekerFixture("profile", VisibilityStatus.PUBLIC, ProfileStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/job-seeker/profile")
                        .with(jwtFor(fixture.keycloakUserId(), "SEEKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(fixture.profileId()))
                .andExpect(jsonPath("$.data.publicProfileSlug").value(fixture.slug()))
                .andExpect(jsonPath("$.data.profileVisibility").value("PUBLIC"));

        mockMvc.perform(patch("/api/v1/job-seeker/profile")
                        .with(jwtFor(fixture.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "headline":" Senior Backend Developer ",
                                  "bio":"Updated profile bio",
                                  "currentPosition":"Platform Engineer",
                                  "expectedSalaryMin":1500,
                                  "expectedSalaryMax":2500,
                                  "expectedSalaryCurrency":"USD",
                                  "salaryVisibility":"RECRUITERS_ONLY",
                                  "preferredLocation":"Phnom Penh",
                                  "availabilityStatus":"OPEN",
                                  "profileVisibility":"PRIVATE",
                                  "verificationStatus":"APPROVED",
                                  "publicProfileSlug":"tampered-slug"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headline").value("Senior Backend Developer"))
                .andExpect(jsonPath("$.data.salaryVisibility").value("RECRUITERS_ONLY"))
                .andExpect(jsonPath("$.data.publicProfileSlug").value(fixture.slug()))
                .andExpect(jsonPath("$.data.profileVisibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING_VERIFICATION"));

        transactionTemplate.executeWithoutResult(status -> {
            JobSeekerProfile profile = entityManager.find(JobSeekerProfile.class, fixture.profileId());
            assertThat(profile.getPublicProfileSlug()).isEqualTo(fixture.slug());
            assertThat(profile.getProfileVisibility()).isEqualTo(VisibilityStatus.PUBLIC);
            assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING_VERIFICATION);
        });
    }

    @Test
    void resumeCrudEnforcesOwnerDefaultAndApplicationReferenceRules() throws Exception {
        Fixture owner = createSeekerFixture("resume-owner", VisibilityStatus.PRIVATE, ProfileStatus.ACTIVE);
        Fixture other = createSeekerFixture("resume-other", VisibilityStatus.PRIVATE, ProfileStatus.ACTIVE);

        String firstResumeResponse = mockMvc.perform(post("/api/v1/job-seeker/resumes")
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":" Primary Resume ",
                                  "resumeFileUrl":"https://files.example/primary.pdf",
                                  "resumeData":{"summary":"primary"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Primary Resume"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long firstResumeId = extractId(firstResumeResponse);

        String secondResumeResponse = mockMvc.perform(post("/api/v1/job-seeker/resumes")
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Secondary Resume",
                                  "resumeFileUrl":"https://files.example/secondary.pdf"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long secondResumeId = extractId(secondResumeResponse);

        mockMvc.perform(post("/api/v1/job-seeker/resumes/{resumeId}/default", firstResumeId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));

        mockMvc.perform(post("/api/v1/job-seeker/resumes/{resumeId}/default", secondResumeId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(entityManager.find(Resume.class, firstResumeId).getIsDefault()).isFalse();
            assertThat(entityManager.find(Resume.class, secondResumeId).getIsDefault()).isTrue();
        });

        Long otherResumeId = createResume(other.profileId(), "Other Resume", "https://files.example/other.pdf");
        mockMvc.perform(get("/api/v1/job-seeker/resumes/{resumeId}", otherResumeId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER")))
                .andExpect(status().isNotFound());

        createApplicationReferencingResume(owner.profileId(), firstResumeId);

        mockMvc.perform(patch("/api/v1/job-seeker/resumes/{resumeId}", firstResumeId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated referenced resume\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated referenced resume"));

        mockMvc.perform(patch("/api/v1/job-seeker/resumes/{resumeId}", firstResumeId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeFileUrl\":\"https://files.example/replaced.pdf\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/job-seeker/resumes/{resumeId}", firstResumeId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER")))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/job-seeker/resumes/{resumeId}", secondResumeId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER")))
                .andExpect(status().isOk());
    }

    @Test
    void portfolioAndProjectCrudAreOwnerScopedAndRemainPrivateUntilPublished() throws Exception {
        Fixture owner = createSeekerFixture("portfolio-owner", VisibilityStatus.PUBLIC, ProfileStatus.ACTIVE);
        Fixture other = createSeekerFixture("portfolio-other", VisibilityStatus.PUBLIC, ProfileStatus.ACTIVE);

        String portfolioResponse = mockMvc.perform(post("/api/v1/job-seeker/portfolios")
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Private Owner Portfolio",
                                  "summary":"Owner-only work",
                                  "publicUrl":"https://portfolio.example/private-owner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long portfolioId = extractId(portfolioResponse);

        String projectResponse = mockMvc.perform(post("/api/v1/job-seeker/portfolios/{portfolioId}/projects", portfolioId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Private Project",
                                  "description":"Hidden implementation detail",
                                  "projectUrl":"https://example.com/project",
                                  "githubUrl":"https://github.com/example/project",
                                  "imageUrl":"https://images.example/project.png",
                                  "techStack":"Java, Spring",
                                  "displayOrder":3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayOrder").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long projectId = extractId(projectResponse);

        mockMvc.perform(patch("/api/v1/job-seeker/portfolios/{portfolioId}/projects/{projectId}", portfolioId, projectId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Private Project Updated\",\"displayOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Private Project Updated"))
                .andExpect(jsonPath("$.data.displayOrder").value(1));

        mockMvc.perform(get("/api/v1/job-seeker/portfolios/{portfolioId}", portfolioId)
                        .with(jwtFor(other.keycloakUserId(), "SEEKER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/job-seeker/portfolios/{portfolioId}/projects/{projectId}", portfolioId, projectId)
                        .with(jwtFor(other.keycloakUserId(), "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"tampered\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/recruiter/talent/{slug}", owner.slug())
                        .with(jwtFor("recruiter-private-portfolio", "RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.portfolios.length()").value(0))
                .andExpect(content().string(not(containsString("Private Owner Portfolio"))))
                .andExpect(content().string(not(containsString("Private Project Updated"))));

        mockMvc.perform(delete("/api/v1/job-seeker/portfolios/{portfolioId}/projects/{projectId}", portfolioId, projectId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/job-seeker/portfolios/{portfolioId}", portfolioId)
                        .with(jwtFor(owner.keycloakUserId(), "SEEKER")))
                .andExpect(status().isOk());
    }

    private Fixture createSeekerFixture(
            String marker,
            VisibilityStatus visibility,
            ProfileStatus status
    ) {
        return transactionTemplate.execute(tx -> {
            int suffix = SEQUENCE.incrementAndGet();

            UserAccount user = new UserAccount();
            user.setKeycloakUserId("seeker-resource-" + marker + "-" + suffix);
            entityManager.persist(user);

            JobSeekerProfile profile = new JobSeekerProfile();
            profile.setUserAccount(user);
            profile.setHeadline("Original " + marker);
            profile.setBio("Original bio " + marker);
            profile.setCurrentPosition("Developer");
            profile.setExpectedSalaryMin(new BigDecimal("1000.00"));
            profile.setExpectedSalaryMax(new BigDecimal("2000.00"));
            profile.setExpectedSalaryCurrency("USD");
            profile.setSalaryVisibility(SalaryVisibility.PRIVATE);
            profile.setPreferredLocation("Phnom Penh");
            profile.setAvailabilityStatus("AVAILABLE");
            profile.setProfileVisibility(visibility);
            profile.setStatus(status);
            profile.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
            if (visibility == VisibilityStatus.PUBLIC) {
                profile.setPublicProfileSlug("resource-talent-" + marker + "-" + suffix);
                profile.setPublishedAt(Instant.now());
            }
            entityManager.persist(profile);
            entityManager.flush();

            return new Fixture(
                    user.getId(),
                    profile.getId(),
                    user.getKeycloakUserId(),
                    profile.getPublicProfileSlug()
            );
        });
    }

    private Long createResume(Long profileId, String title, String fileUrl) {
        return transactionTemplate.execute(status -> {
            JobSeekerProfile profile = entityManager.find(JobSeekerProfile.class, profileId);
            Resume resume = new Resume();
            resume.setJobSeekerProfile(profile);
            resume.setTitle(title);
            resume.setResumeFileUrl(fileUrl);
            resume.setVisibility(VisibilityStatus.PRIVATE);
            entityManager.persist(resume);
            entityManager.flush();
            return resume.getId();
        });
    }

    private void createApplicationReferencingResume(Long profileId, Long resumeId) {
        transactionTemplate.executeWithoutResult(status -> {
            UserAccount recruiterUser = new UserAccount();
            recruiterUser.setKeycloakUserId("resource-recruiter-" + SEQUENCE.incrementAndGet());
            entityManager.persist(recruiterUser);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(recruiterUser);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Resource Company " + SEQUENCE.incrementAndGet());
            entityManager.persist(company);

            JobPost jobPost = new JobPost();
            jobPost.setCompany(company);
            jobPost.setRecruiterProfile(recruiterProfile);
            jobPost.setTitle("Resource Job");
            jobPost.setDescription("Resource job description");
            jobPost.setStatus(JobStatus.PUBLISHED);
            jobPost.setPublishedAt(Instant.now());
            entityManager.persist(jobPost);

            JobApplication application = new JobApplication();
            application.setJobPost(jobPost);
            application.setJobSeekerProfile(entityManager.find(JobSeekerProfile.class, profileId));
            application.setResume(entityManager.find(Resume.class, resumeId));
            entityManager.persist(application);
        });
    }

    private Long extractId(String responseBody) {
        int idIndex = responseBody.indexOf("\"id\":");
        int start = idIndex + 5;
        int end = start;
        while (end < responseBody.length() && Character.isDigit(responseBody.charAt(end))) {
            end++;
        }
        return Long.parseLong(responseBody.substring(start, end));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(String subject, String role) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject(subject)
                        .claim("realm_access", java.util.Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private record Fixture(
            Long userAccountId,
            Long profileId,
            String keycloakUserId,
            String slug
    ) {
    }
}
