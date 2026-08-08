package co.istad.ai_interview_app.features.moderator;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModeratorCompanyVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void moderatorCanListAndInspectCompanies() throws Exception {
        RecruiterFixture recruiter = createRecruiterCompany("list-view", VerificationStatus.PENDING_VERIFICATION, ProfileStatus.PENDING);
        createModerator("moderator-list-view");

        mockMvc.perform(get("/api/v1/moderator/companies")
                        .with(jwtFor("moderator-list-view", "MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].name", hasItem("Company list-view")))
                .andExpect(jsonPath("$.data.content[*].verificationStatus", hasItem("PENDING_VERIFICATION")));

        mockMvc.perform(get("/api/v1/moderator/companies/{companyId}", recruiter.companyId())
                        .with(jwtFor("moderator-list-view", "MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.company.id").value(recruiter.companyId()));
    }

    @Test
    void moderatorCanRejectCompanyVerification() throws Exception {
        RecruiterFixture recruiter = createRecruiterCompany("reject-flow", VerificationStatus.PENDING_VERIFICATION, ProfileStatus.PENDING);
        createModerator("moderator-reject-flow");

        mockMvc.perform(post("/api/v1/moderator/companies/{companyId}/reject", recruiter.companyId())
                        .with(jwtFor("moderator-reject-flow", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionNote": "Missing registration document"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("REJECTED"));

        transactionTemplate.executeWithoutResult(status -> {
            Company company = entityManager.find(Company.class, recruiter.companyId());
            assertThat(company.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
            assertThat(company.getStatus()).isEqualTo(ProfileStatus.PENDING);
        });
    }

    @Test
    void moderatorCanRequestRevisionForCompanyVerification() throws Exception {
        RecruiterFixture recruiter = createRecruiterCompany("revision-flow", VerificationStatus.PENDING_VERIFICATION, ProfileStatus.PENDING);
        createModerator("moderator-revision-flow");

        mockMvc.perform(post("/api/v1/moderator/companies/{companyId}/request-revision", recruiter.companyId())
                        .with(jwtFor("moderator-revision-flow", "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionNote": "Please update your business registration"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("NEEDS_REVISION"));

        transactionTemplate.executeWithoutResult(status -> {
            Company company = entityManager.find(Company.class, recruiter.companyId());
            assertThat(company.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING_VERIFICATION);
            assertThat(company.getStatus()).isEqualTo(ProfileStatus.PENDING);
        });
    }

    private RecruiterFixture createRecruiterCompany(String marker, VerificationStatus verificationStatus, ProfileStatus companyStatus) {
        return transactionTemplate.execute(status -> {
            UserAccount user = new UserAccount();
            user.setKeycloakUserId("recruiter-" + marker + "-" + System.nanoTime());
            entityManager.persist(user);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(user);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Company " + marker);
            company.setVerificationStatus(verificationStatus);
            company.setStatus(companyStatus);
            entityManager.persist(company);
            entityManager.flush();

            return new RecruiterFixture(user.getKeycloakUserId(), recruiterProfile.getId(), company.getId());
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

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(String subject, String role) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject(subject)
                        .claim("realm_access", java.util.Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private record RecruiterFixture(String keycloakUserId, Long recruiterProfileId, Long companyId) {
    }
}
