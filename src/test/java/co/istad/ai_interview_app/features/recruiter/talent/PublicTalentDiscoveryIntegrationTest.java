package co.istad.ai_interview_app.features.recruiter.talent;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Portfolio;
import co.istad.ai_interview_app.features.seeker.entity.PortfolioProject;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.profile.SalaryVisibility;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicTalentDiscoveryIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void seekerCanPublishOwnProfileAndCannotPublishAnotherSeekersPortfolio() throws Exception {
        Fixture owner = createTalentFixture("owner", VisibilityStatus.PRIVATE, ProfileStatus.ACTIVE, true);
        Fixture other = createTalentFixture("other", VisibilityStatus.PRIVATE, ProfileStatus.ACTIVE, true);

        mockMvc.perform(patch("/api/v1/job-seeker/profile/publication")
                        .with(jwtFor(owner.keycloakUserId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.publicProfileSlug").value("talent-" + owner.profileId));

        mockMvc.perform(patch("/api/v1/job-seeker/portfolios/{portfolioId}/publication", other.portfolioId)
                        .with(jwtFor(owner.keycloakUserId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resumeCannotBePublishedWithoutFileUrl() throws Exception {
        Fixture fixture = createTalentFixture("resume-file-required", VisibilityStatus.PUBLIC, ProfileStatus.ACTIVE, false);

        mockMvc.perform(patch("/api/v1/job-seeker/resumes/{resumeId}/publication", fixture.resumeId)
                        .with(jwtFor(fixture.keycloakUserId, "SEEKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isBadRequest());

        transactionTemplate.executeWithoutResult(status -> {
            Resume resume = entityManager.find(Resume.class, fixture.resumeId);
            assertThat(resume.getVisibility()).isEqualTo(VisibilityStatus.PRIVATE);
        });
    }

    @Test
    void recruiterTalentListReturnsOnlyActivePublicProfiles() throws Exception {
        String marker = "discoverable-keyword-" + SEQUENCE.incrementAndGet();
        Fixture publicActive = createTalentFixture(marker, VisibilityStatus.PUBLIC, ProfileStatus.ACTIVE, true);
        createTalentFixture(marker + "-private", VisibilityStatus.PRIVATE, ProfileStatus.ACTIVE, true);
        createTalentFixture(marker + "-inactive", VisibilityStatus.PUBLIC, ProfileStatus.INACTIVE, true);

        mockMvc.perform(get("/api/v1/recruiter/talent")
                        .param("keyword", marker)
                        .with(jwtFor("recruiter-list", "RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].profileId").value(publicActive.profileId))
                .andExpect(jsonPath("$.data.content[0].publicProfileSlug").value(publicActive.slug));
    }

    @Test
    void recruiterTalentListRejectsUnsupportedSortBeforeHibernateQuery() throws Exception {
        mockMvc.perform(get("/api/v1/recruiter/talent")
                        .param("keyword", "string")
                        .param("preferredLocation", "string")
                        .param("availabilityStatus", "string")
                        .param("page", "264")
                        .param("size", "100")
                        .param("sort", "string,string")
                        .with(jwtFor("recruiter-invalid-sort", "RECRUITER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported sort property: string"));
    }

    @Test
    void recruiterTalentListRejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/recruiter/talent")
                        .param("size", "6790")
                        .with(jwtFor("recruiter-large-page", "RECRUITER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be less than or equal to 100"));
    }

    @Test
    void recruiterTalentListAllowsSupportedSort() throws Exception {
        String marker = "supported-sort-" + SEQUENCE.incrementAndGet();
        createTalentFixture(marker, VisibilityStatus.PUBLIC, ProfileStatus.ACTIVE, true);

        mockMvc.perform(get("/api/v1/recruiter/talent")
                        .param("keyword", marker)
                        .param("sort", "headline,asc")
                        .with(jwtFor("recruiter-supported-sort", "RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void recruiterRoleIsRequiredForTalentDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/recruiter/talent")
                        .with(jwtFor("seeker-user", "SEEKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCanDownloadOnlyPublicResumeBelongingToPublicProfile() throws Exception {
        Fixture fixture = createTalentFixture("download", VisibilityStatus.PUBLIC, ProfileStatus.ACTIVE, true);
        Long privateResumeId = createResume(fixture.profileId, "Private Resume", "https://files.example/private.pdf", VisibilityStatus.PRIVATE);

        mockMvc.perform(get("/api/v1/recruiter/talent/{slug}/resumes/{resumeId}/download", fixture.slug, fixture.resumeId)
                        .with(jwtFor("recruiter-download", "RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value(fixture.resumeId))
                .andExpect(jsonPath("$.data.downloadUrl").value("https://files.example/" + fixture.slug + ".pdf"));

        mockMvc.perform(get("/api/v1/recruiter/talent/{slug}/resumes/{resumeId}/download", fixture.slug, privateResumeId)
                        .with(jwtFor("recruiter-download", "RECRUITER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/recruiter/talent/{slug}/resumes/{resumeId}/download", fixture.slug, fixture.resumeId)
                        .with(jwtFor("seeker-download", "SEEKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void talentDetailContainsOnlyPublicResourcesAndNoApplicationOrAiData() throws Exception {
        Fixture fixture = createTalentFixture("leakage", VisibilityStatus.PUBLIC, ProfileStatus.ACTIVE, true);
        createResume(fixture.profileId, "Private Resume Leak", "https://files.example/private-leak.pdf", VisibilityStatus.PRIVATE);
        createPrivateWorkflowData(fixture.profileId, fixture.userAccountId, "secret-cover-letter", "secret-ai-transcript");

        mockMvc.perform(get("/api/v1/recruiter/talent/{slug}", fixture.slug)
                        .with(jwtFor("recruiter-detail", "RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.publicProfileSlug").value(fixture.slug))
                .andExpect(jsonPath("$.data.profile.expectedSalaryMin").doesNotExist())
                .andExpect(jsonPath("$.data.portfolios.length()").value(1))
                .andExpect(jsonPath("$.data.portfolios[0].projects.length()").value(1))
                .andExpect(jsonPath("$.data.resumes.length()").value(1))
                .andExpect(jsonPath("$.data.resumes[0].id").value(fixture.resumeId))
                .andExpect(content().string(not(containsString("Private Resume Leak"))))
                .andExpect(content().string(not(containsString("private-leak.pdf"))))
                .andExpect(content().string(not(containsString("secret-cover-letter"))))
                .andExpect(content().string(not(containsString("secret-ai-transcript"))))
                .andExpect(content().string(not(containsString(fixture.keycloakUserId))));
    }

    private Fixture createTalentFixture(
            String marker,
            VisibilityStatus profileVisibility,
            ProfileStatus profileStatus,
            boolean resumeHasFile
    ) {
        return transactionTemplate.execute(status -> {
            int suffix = SEQUENCE.incrementAndGet();
            UserAccount user = new UserAccount();
            user.setKeycloakUserId("seeker-" + marker + "-" + suffix);
            entityManager.persist(user);

            JobSeekerProfile profile = new JobSeekerProfile();
            profile.setUserAccount(user);
            profile.setHeadline("Backend Developer " + marker);
            profile.setBio("Builds APIs for " + marker);
            profile.setCurrentPosition("Java Developer");
            profile.setExpectedSalaryMin(new BigDecimal("1000.00"));
            profile.setExpectedSalaryMax(new BigDecimal("2000.00"));
            profile.setExpectedSalaryCurrency("USD");
            profile.setSalaryVisibility(SalaryVisibility.PRIVATE);
            profile.setPreferredLocation("Phnom Penh");
            profile.setAvailabilityStatus("AVAILABLE");
            if (profileVisibility == VisibilityStatus.PUBLIC) {
                profile.setPublicProfileSlug("talent-" + marker + "-" + suffix);
            }
            profile.setProfileVisibility(profileVisibility);
            profile.setStatus(profileStatus);
            if (profileVisibility == VisibilityStatus.PUBLIC) {
                profile.setPublishedAt(Instant.now());
            }
            entityManager.persist(profile);

            Portfolio portfolio = new Portfolio();
            portfolio.setJobSeekerProfile(profile);
            portfolio.setTitle("Public Portfolio " + marker);
            portfolio.setSummary("Portfolio summary " + marker);
            portfolio.setPublicUrl("https://portfolio.example/" + marker + "-" + suffix);
            portfolio.setVisibility(VisibilityStatus.PUBLIC);
            portfolio.setPublishedAt(Instant.now());
            portfolio.setStatus(ProfileStatus.ACTIVE);
            entityManager.persist(portfolio);

            PortfolioProject project = new PortfolioProject();
            project.setPortfolio(portfolio);
            project.setTitle("Public Project " + marker);
            project.setDescription("Project description " + marker);
            project.setDisplayOrder(1);
            entityManager.persist(project);

            Resume resume = new Resume();
            resume.setJobSeekerProfile(profile);
            resume.setTitle("Public Resume " + marker);
            String resumeSlug = profile.getPublicProfileSlug() != null
                    ? profile.getPublicProfileSlug()
                    : "draft-" + marker + "-" + suffix;
            resume.setResumeFileUrl(resumeHasFile ? "https://files.example/" + resumeSlug + ".pdf" : null);
            resume.setVisibility(resumeHasFile ? VisibilityStatus.PUBLIC : VisibilityStatus.PRIVATE);
            if (resumeHasFile) {
                resume.setPublishedAt(Instant.now());
            }
            entityManager.persist(resume);

            entityManager.flush();

            return new Fixture(
                    user.getId(),
                    profile.getId(),
                    portfolio.getId(),
                    resume.getId(),
                    user.getKeycloakUserId(),
                    profile.getPublicProfileSlug()
            );
        });
    }

    private Long createResume(Long profileId, String title, String fileUrl, VisibilityStatus visibility) {
        return transactionTemplate.execute(status -> {
            JobSeekerProfile profile = entityManager.find(JobSeekerProfile.class, profileId);
            Resume resume = new Resume();
            resume.setJobSeekerProfile(profile);
            resume.setTitle(title);
            resume.setResumeFileUrl(fileUrl);
            resume.setVisibility(visibility);
            if (visibility == VisibilityStatus.PUBLIC) {
                resume.setPublishedAt(Instant.now());
            }
            entityManager.persist(resume);
            entityManager.flush();
            return resume.getId();
        });
    }

    private void createPrivateWorkflowData(
            Long profileId,
            Long seekerUserAccountId,
            String coverLetter,
            String transcript
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            JobSeekerProfile profile = entityManager.find(JobSeekerProfile.class, profileId);
            UserAccount seekerUser = entityManager.find(UserAccount.class, seekerUserAccountId);

            UserAccount recruiterUser = new UserAccount();
            recruiterUser.setKeycloakUserId("workflow-recruiter-" + SEQUENCE.incrementAndGet());
            entityManager.persist(recruiterUser);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(recruiterUser);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Workflow Company");
            entityManager.persist(company);

            JobPost jobPost = new JobPost();
            jobPost.setCompany(company);
            jobPost.setRecruiterProfile(recruiterProfile);
            jobPost.setTitle("Workflow Job");
            jobPost.setDescription("Workflow description");
            jobPost.setStatus(JobStatus.PUBLISHED);
            jobPost.setPublishedAt(Instant.now());
            entityManager.persist(jobPost);

            JobApplication application = new JobApplication();
            application.setJobPost(jobPost);
            application.setJobSeekerProfile(profile);
            application.setCoverLetter(coverLetter);
            entityManager.persist(application);

            AiInterviewSession session = new AiInterviewSession();
            session.setApplication(application);
            session.setJobPost(jobPost);
            session.setJobSeeker(seekerUser);
            session.setProvider("test-provider");
            session.setAiModel("test-model");
            session.setTranscript(transcript);
            entityManager.persist(session);
        });
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
            Long portfolioId,
            Long resumeId,
            String keycloakUserId,
            String slug
    ) {
    }
}
