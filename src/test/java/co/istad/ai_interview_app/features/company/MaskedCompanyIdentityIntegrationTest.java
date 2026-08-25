package co.istad.ai_interview_app.features.company;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.FavoriteJob;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What a candidate can learn about a masked company.
 *
 * <p>Written as leak hunting rather than feature checking: the interesting
 * failure is not "the detail page forgot to mask", it is some other endpoint
 * nobody thought about handing the name back. Each test names a route out.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaskedCompanyIdentityIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void aMaskedCompanyIsNotNamedOnItsJobDetail() throws Exception {
        Seed seed = seed("Masked Acme", CompanyIdentityVisibility.MASKED);

        mockMvc.perform(get("/api/v1/public/jobs/{jobId}", seed.jobId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Confidential company"))
                // The id is a handle back to the company, so it is withheld too.
                .andExpect(jsonPath("$.data.companyId").doesNotExist())
                .andExpect(content().string(not(containsString(seed.companyName()))));
    }

    @Test
    void aVisibleCompanyIsStillNamed() throws Exception {
        Seed seed = seed("Named Globex", CompanyIdentityVisibility.VISIBLE);

        mockMvc.perform(get("/api/v1/public/jobs/{jobId}", seed.jobId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value(seed.companyName()))
                .andExpect(jsonPath("$.data.companyId").value(seed.companyId()));
    }

    /**
     * Searching the masked name must not find the job. Otherwise the search box
     * answers the question the mask exists to refuse.
     */
    @Test
    void aMaskedCompanyNameIsNotSearchable() throws Exception {
        Seed seed = seed("Unfindable Industries", CompanyIdentityVisibility.MASKED);

        mockMvc.perform(get("/api/v1/public/jobs").param("keyword", seed.companyName()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(seed.companyName()))))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    /** The public skills list credits whoever added a skill — by company. */
    @Test
    void aMaskedCompanyIsNotNamedAsTheAuthorOfASkill() throws Exception {
        Seed seed = seed("Skill Author Co", CompanyIdentityVisibility.MASKED);

        transactionTemplate.executeWithoutResult(status -> {
            Skill skill = new Skill();
            skill.setName("Masked skill " + SEQUENCE.incrementAndGet());
            skill.setSkillType("TECHNICAL");
            skill.setCreatedByRecruiterProfile(
                    entityManager.find(RecruiterProfile.class, seed.recruiterProfileId()));
            entityManager.persist(skill);
        });

        mockMvc.perform(get("/api/v1/public/skills"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(seed.companyName()))));
    }

    /**
     * A job saved before the company was masked is still in the candidate's
     * list, and must not keep showing the name it was saved under.
     */
    @Test
    void aMaskedCompanyIsNotNamedOnASavedJob() throws Exception {
        Seed seed = seed("Saved Job Co", CompanyIdentityVisibility.MASKED);
        String seekerKeycloakId = "mask-seeker-" + SEQUENCE.incrementAndGet();

        transactionTemplate.executeWithoutResult(status -> {
            UserAccount seekerUser = new UserAccount();
            seekerUser.setKeycloakUserId(seekerKeycloakId);
            entityManager.persist(seekerUser);

            JobSeekerProfile profile = new JobSeekerProfile();
            profile.setUserAccount(seekerUser);
            entityManager.persist(profile);

            FavoriteJob favorite = new FavoriteJob();
            favorite.setJobSeekerProfile(profile);
            favorite.setJobPost(entityManager.find(JobPost.class, seed.jobId()));
            entityManager.persist(favorite);
        });

        mockMvc.perform(get("/api/v1/job-seeker/favorite-jobs")
                        .with(jwtFor(seekerKeycloakId, "SEEKER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(seed.companyName()))))
                .andExpect(jsonPath("$.data.content[0].companyName").value("Confidential company"));
    }

    @Test
    void anAdministratorCanMaskAndUnmaskACompany() throws Exception {
        Seed seed = seed("Toggle Co", CompanyIdentityVisibility.VISIBLE);

        mockMvc.perform(patch("/api/v1/moderator/companies/{id}/identity-visibility", seed.companyId())
                        .with(moderatorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"MASKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.company.identityVisibility").value("MASKED"));

        mockMvc.perform(get("/api/v1/public/jobs/{jobId}", seed.jobId()))
                .andExpect(jsonPath("$.data.companyName").value("Confidential company"));

        mockMvc.perform(patch("/api/v1/moderator/companies/{id}/identity-visibility", seed.companyId())
                        .with(moderatorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"VISIBLE\"}"))
                .andExpect(status().isOk());

        // Unmasking is immediate: nothing was copied onto the job posts.
        mockMvc.perform(get("/api/v1/public/jobs/{jobId}", seed.jobId()))
                .andExpect(jsonPath("$.data.companyName").value(seed.companyName()));
    }

    /** Masking is the administrator's call, not the masked company's. */
    @Test
    void aRecruiterCannotChangeTheirOwnMasking() throws Exception {
        Seed seed = seed("Self Serve Co", CompanyIdentityVisibility.MASKED);

        mockMvc.perform(patch("/api/v1/moderator/companies/{id}/identity-visibility", seed.companyId())
                        .with(recruiterJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"VISIBLE\"}"))
                .andExpect(status().isForbidden());
    }

    /* ------------------------------------------------------------ seed --- */

    private Seed seed(String companyName, CompanyIdentityVisibility visibility) {
        int suffix = SEQUENCE.incrementAndGet();
        String uniqueName = companyName + " " + suffix;

        return transactionTemplate.execute(status -> {
            UserAccount recruiterUser = new UserAccount();
            recruiterUser.setKeycloakUserId("mask-recruiter-" + suffix);
            entityManager.persist(recruiterUser);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(recruiterUser);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName(uniqueName);
            // Public listings only return approved, active companies.
            company.setVerificationStatus(VerificationStatus.APPROVED);
            company.setStatus(ProfileStatus.ACTIVE);
            company.setIdentityVisibility(visibility);
            entityManager.persist(company);

            JobPost jobPost = new JobPost();
            jobPost.setCompany(company);
            jobPost.setRecruiterProfile(recruiterProfile);
            jobPost.setTitle("Masking Test Role " + suffix);
            jobPost.setDescription("A role at a company that may or may not be named.");
            jobPost.setStatus(JobStatus.PUBLISHED);
            jobPost.setPublishedAt(Instant.now());
            entityManager.persist(jobPost);
            entityManager.flush();

            return new Seed(company.getId(), uniqueName, jobPost.getId(), recruiterProfile.getId());
        });
    }

    private static RequestPostProcessor moderatorJwt() {
        return jwtFor("mask-moderator", "MODERATOR");
    }

    private static RequestPostProcessor recruiterJwt() {
        return jwtFor("mask-recruiter-caller", "RECRUITER");
    }

    private static RequestPostProcessor jwtFor(String subject, String role) {
        return jwt()
                .jwt(token -> token
                        .subject(subject)
                        .claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private record Seed(Long companyId, String companyName, Long jobId, Long recruiterProfileId) {
    }
}
