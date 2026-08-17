package co.istad.ai_interview_app.features.job;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.entity.CompanyDocument;
import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.job.entity.JobCategory;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
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
class CompanyVerificationJobPublicationIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void unverifiedCompanyCannotPublish() throws Exception {
        RecruiterFixture recruiter = createRecruiterCompany(
                "unverified",
                VerificationStatus.PENDING_VERIFICATION,
                ProfileStatus.PENDING
        );
        Long jobId = createJob(recruiter.recruiterProfileId(), recruiter.companyId(), "Unverified Publish", JobStatus.DRAFT, future());

        mockMvc.perform(post("/api/v1/recruiter/jobs/{id}/publish", jobId)
                        .with(jwtFor(recruiter.keycloakUserId(), "RECRUITER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Company must be approved before publishing jobs"));
    }

    @Test
    void approvedCompanyCanPublishDirectlyAfterDocumentSubmissionAndModeratorDecision() throws Exception {
        RecruiterFixture recruiter = createRecruiterCompany(
                "approved-flow",
                VerificationStatus.PENDING_VERIFICATION,
                ProfileStatus.PENDING
        );
        createModerator("moderator-approved-flow");

        mockMvc.perform(post("/api/v1/recruiter/companies/{companyId}/submit-verification", recruiter.companyId())
                        .with(jwtFor(recruiter.keycloakUserId(), "RECRUITER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Company verification requires a business registration document"));

        mockMvc.perform(post("/api/v1/recruiter/companies/{companyId}/documents", recruiter.companyId())
                        .with(jwtFor(recruiter.keycloakUserId(), "RECRUITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType":"business-registration",
                                  "documentUrl":"https://files.example/company-secret-registration.pdf"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentType").value("BUSINESS_REGISTRATION"));

        mockMvc.perform(post("/api/v1/recruiter/companies/{companyId}/submit-verification", recruiter.companyId())
                        .with(jwtFor(recruiter.keycloakUserId(), "RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING_VERIFICATION"));

        mockMvc.perform(post("/api/v1/moderator/companies/{companyId}/approve", recruiter.companyId())
                        .with(jwtFor("moderator-approved-flow", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("APPROVED"));

        Long jobId = createJob(recruiter.recruiterProfileId(), recruiter.companyId(), "Approved Direct Publish", JobStatus.DRAFT, future());

        mockMvc.perform(post("/api/v1/recruiter/jobs/{id}/publish", jobId)
                        .with(jwtFor(recruiter.keycloakUserId(), "RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        transactionTemplate.executeWithoutResult(status -> {
            Company company = entityManager.find(Company.class, recruiter.companyId());
            assertThat(company.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
            assertThat(company.getStatus()).isEqualTo(ProfileStatus.ACTIVE);

            Long historyCount = entityManager
                    .createQuery("""
                            select count(v)
                            from CompanyVerification v
                            where v.company.id = :companyId
                            """, Long.class)
                    .setParameter("companyId", recruiter.companyId())
                    .getSingleResult();
            assertThat(historyCount).isEqualTo(1);
        });
    }

    @Test
    void recruiterCannotPublishAnotherRecruitersJob() throws Exception {
        RecruiterFixture owner = createRecruiterCompany("owner", VerificationStatus.APPROVED, ProfileStatus.ACTIVE);
        RecruiterFixture other = createRecruiterCompany("other", VerificationStatus.APPROVED, ProfileStatus.ACTIVE);
        Long jobId = createJob(owner.recruiterProfileId(), owner.companyId(), "Owned Job", JobStatus.DRAFT, future());

        mockMvc.perform(post("/api/v1/recruiter/jobs/{id}/publish", jobId)
                        .with(jwtFor(other.keycloakUserId(), "RECRUITER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicJobDiscoveryExcludesPrivateInactiveAndExpiredJobsWithoutLeakingPrivateData() throws Exception {
        String marker = "public-filter-" + SEQUENCE.incrementAndGet();
        RecruiterFixture approved = createRecruiterCompany("public-approved", VerificationStatus.APPROVED, ProfileStatus.ACTIVE);
        RecruiterFixture unapproved = createRecruiterCompany("public-unapproved", VerificationStatus.PENDING_VERIFICATION, ProfileStatus.PENDING);
        RecruiterFixture suspended = createRecruiterCompany("public-suspended", VerificationStatus.APPROVED, ProfileStatus.SUSPENDED);
        addCompanyDocument(approved.companyId(), approved.recruiterProfileId(), "https://files.example/private-company-doc.pdf");

        Long publicJobId = createJob(
                approved.recruiterProfileId(),
                approved.companyId(),
                "Visible " + marker,
                JobStatus.PUBLISHED,
                future()
        );
        createJob(approved.recruiterProfileId(), approved.companyId(), "Draft " + marker, JobStatus.DRAFT, future());
        createJob(approved.recruiterProfileId(), approved.companyId(), "Paused " + marker, JobStatus.PAUSED, future());
        createJob(approved.recruiterProfileId(), approved.companyId(), "Closed " + marker, JobStatus.CLOSED, future());
        Long expiredJobId = createJob(
                approved.recruiterProfileId(),
                approved.companyId(),
                "Expired " + marker,
                JobStatus.PUBLISHED,
                Instant.now().minus(1, ChronoUnit.DAYS)
        );
        Long unapprovedJobId = createJob(
                unapproved.recruiterProfileId(),
                unapproved.companyId(),
                "Unapproved " + marker,
                JobStatus.PUBLISHED,
                future()
        );
        Long suspendedJobId = createJob(
                suspended.recruiterProfileId(),
                suspended.companyId(),
                "Suspended " + marker,
                JobStatus.PUBLISHED,
                future()
        );

        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(publicJobId))
                .andExpect(content().string(not(containsString(approved.keycloakUserId()))))
                .andExpect(content().string(not(containsString("private-company-doc.pdf"))))
                .andExpect(content().string(not(containsString("verificationHistory"))));

        mockMvc.perform(get("/api/v1/public/jobs/{jobId}", publicJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(publicJobId));

        mockMvc.perform(get("/api/v1/public/jobs/{jobId}", expiredJobId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/jobs/{jobId}", unapprovedJobId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/jobs/{jobId}", suspendedJobId))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicReferenceDataEndpointsArePermitAll() throws Exception {
        ReferenceFixture fixture = createReferenceData();

        mockMvc.perform(get("/api/v1/public/job-categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(fixture.categoryName())));

        mockMvc.perform(get("/api/v1/public/skills"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(fixture.skillName())));

        mockMvc.perform(get("/api/v1/public/industries"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(fixture.industryName())));
    }

    @Test
    void publicJobListRejectsOversizedPageAndUnsupportedSort() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be less than or equal to 100"));

        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("sort", "recruiterProfile.userAccount.keycloakUserId,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Unsupported sort property: recruiterProfile.userAccount.keycloakUserId"
                ));
    }

    private RecruiterFixture createRecruiterCompany(
            String marker,
            VerificationStatus verificationStatus,
            ProfileStatus companyStatus
    ) {
        return transactionTemplate.execute(status -> {
            int suffix = SEQUENCE.incrementAndGet();

            UserAccount user = new UserAccount();
            user.setKeycloakUserId("recruiter-" + marker + "-" + suffix);
            entityManager.persist(user);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(user);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Company " + marker + " " + suffix);
            company.setVerificationStatus(verificationStatus);
            company.setStatus(companyStatus);
            entityManager.persist(company);
            entityManager.flush();

            return new RecruiterFixture(
                    user.getKeycloakUserId(),
                    recruiterProfile.getId(),
                    company.getId()
            );
        });
    }

    private Long createJob(
            Long recruiterProfileId,
            Long companyId,
            String title,
            JobStatus status,
            Instant expiredAt
    ) {
        return transactionTemplate.execute(transactionStatus -> {
            RecruiterProfile recruiterProfile = entityManager.find(RecruiterProfile.class, recruiterProfileId);
            Company company = entityManager.find(Company.class, companyId);

            JobPost jobPost = new JobPost();
            jobPost.setRecruiterProfile(recruiterProfile);
            jobPost.setCompany(company);
            jobPost.setTitle(title);
            jobPost.setDescription("Description for " + title);
            jobPost.setStatus(status);
            jobPost.setExpiredAt(expiredAt);
            if (status == JobStatus.PUBLISHED) {
                jobPost.setPublishedAt(Instant.now());
            }
            entityManager.persist(jobPost);
            entityManager.flush();
            return jobPost.getId();
        });
    }

    private void createModerator(String keycloakUserId) {
        transactionTemplate.executeWithoutResult(status -> {
            UserAccount user = new UserAccount();
            user.setKeycloakUserId(keycloakUserId);
            entityManager.persist(user);

            ModeratorProfile moderatorProfile = new ModeratorProfile();
            moderatorProfile.setUserAccount(user);
            entityManager.persist(moderatorProfile);
        });
    }

    private void addCompanyDocument(Long companyId, Long recruiterProfileId, String documentUrl) {
        transactionTemplate.executeWithoutResult(status -> {
            Company company = entityManager.find(Company.class, companyId);
            RecruiterProfile recruiterProfile = entityManager.find(RecruiterProfile.class, recruiterProfileId);

            CompanyDocument document = new CompanyDocument();
            document.setCompany(company);
            document.setUploadedByRecruiterProfile(recruiterProfile);
            document.setDocumentType("BUSINESS_REGISTRATION");
            document.setDocumentUrl(documentUrl);
            document.setStatus(ProfileStatus.ACTIVE);
            entityManager.persist(document);
        });
    }

    private ReferenceFixture createReferenceData() {
        return transactionTemplate.execute(status -> {
            int suffix = SEQUENCE.incrementAndGet();

            JobCategory category = new JobCategory();
            category.setName("Category " + suffix);
            category.setDescription("Category description");
            entityManager.persist(category);

            Skill skill = new Skill();
            skill.setName("Skill " + suffix);
            skill.setSkillType("TECHNICAL");
            entityManager.persist(skill);

            Industry industry = new Industry();
            industry.setName("Industry " + suffix);
            industry.setDescription("Industry description");
            industry.setStatus(ProfileStatus.ACTIVE);
            entityManager.persist(industry);
            entityManager.flush();

            return new ReferenceFixture(category.getName(), skill.getName(), industry.getName());
        });
    }

    private Instant future() {
        return Instant.now().plus(7, ChronoUnit.DAYS);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(String subject, String role) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject(subject)
                        .claim("realm_access", java.util.Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private record RecruiterFixture(
            String keycloakUserId,
            Long recruiterProfileId,
            Long companyId
    ) {
    }

    private record ReferenceFixture(
            String categoryName,
            String skillName,
            String industryName
    ) {
    }
}
