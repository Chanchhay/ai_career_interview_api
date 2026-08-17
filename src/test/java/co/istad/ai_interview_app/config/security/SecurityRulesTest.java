package co.istad.ai_interview_app.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Pins the URL rules in {@link SecurityConfig}, which is the only place
 * authorization is expressed now that the controllers carry no annotations.
 *
 * <p>Assertions are about 403 and nothing else. A permitted call may still fail
 * further in — a missing profile, an unknown id — so "allowed" is asserted as
 * "not FORBIDDEN" rather than as 200. What is being tested is the rule, not the
 * handler behind it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRulesTest {

    private static final String MODERATOR_PATH = "/api/v1/moderator/companies";
    private static final String TAXONOMY_PATH = "/api/v1/admin/industries";
    private static final String SEEKER_PATH = "/api/v1/job-seeker/profile";
    private static final String RECRUITER_PATH = "/api/v1/recruiter/profile";
    private static final String IDENTITY_PATH = "/api/v1/me";
    private static final String PUBLIC_PATH = "/api/v1/public/jobs";

    @Autowired
    private MockMvc mockMvc;

    /* ------------------------------------------------------ super admin --- */

    @Test
    @DisplayName("SUPER_ADMIN reaches moderator endpoints through the role hierarchy")
    void superAdminReachesModeratorArea() throws Exception {
        assertAllowed(MODERATOR_PATH, "SUPER_ADMIN");
    }

    @Test
    @DisplayName("SUPER_ADMIN reaches the shared taxonomy")
    void superAdminReachesTaxonomy() throws Exception {
        assertAllowed(TAXONOMY_PATH, "SUPER_ADMIN");
    }

    @Test
    @DisplayName("SUPER_ADMIN is admitted to the seeker and recruiter areas by name")
    void superAdminReachesProfileAreas() throws Exception {
        assertAllowed(SEEKER_PATH, "SUPER_ADMIN");
        assertAllowed(RECRUITER_PATH, "SUPER_ADMIN");
    }

    /* --------------------------------------------------------- moderator --- */

    @Test
    @DisplayName("MODERATOR reaches review queues and taxonomy, but not seeker or recruiter areas")
    void moderatorScope() throws Exception {
        assertAllowed(MODERATOR_PATH, "MODERATOR");
        assertAllowed(TAXONOMY_PATH, "MODERATOR");
        assertForbidden(SEEKER_PATH, "MODERATOR");
        assertForbidden(RECRUITER_PATH, "MODERATOR");
    }

    /* ---------------------------------------------------- seeker/recruiter --- */

    @Test
    @DisplayName("SEEKER reaches only the seeker area")
    void seekerScope() throws Exception {
        assertAllowed(SEEKER_PATH, "SEEKER");
        assertForbidden(RECRUITER_PATH, "SEEKER");
        assertForbidden(MODERATOR_PATH, "SEEKER");
        assertForbidden(TAXONOMY_PATH, "SEEKER");
    }

    @Test
    @DisplayName("The legacy JOB_SEEKER spelling still reaches the seeker area")
    void legacySeekerRoleStillWorks() throws Exception {
        assertAllowed(SEEKER_PATH, "JOB_SEEKER");
    }

    @Test
    @DisplayName("RECRUITER reaches only the recruiter area")
    void recruiterScope() throws Exception {
        assertAllowed(RECRUITER_PATH, "RECRUITER");
        assertForbidden(SEEKER_PATH, "RECRUITER");
        assertForbidden(MODERATOR_PATH, "RECRUITER");
    }

    @Test
    @DisplayName("FINANCE reaches no other role's area")
    void financeScope() throws Exception {
        assertForbidden(MODERATOR_PATH, "FINANCE");
        assertForbidden(SEEKER_PATH, "FINANCE");
        assertForbidden(RECRUITER_PATH, "FINANCE");
    }

    /* ------------------------------------------------- open and anonymous --- */

    @Test
    @DisplayName("Identity is open to any signed-in account, whatever its role")
    void identityIsOpenToEverySignedInAccount() throws Exception {
        assertAllowed(IDENTITY_PATH, "SEEKER");
        assertAllowed(IDENTITY_PATH, "RECRUITER");
        assertAllowed(IDENTITY_PATH, "FINANCE");
    }

    @Test
    @DisplayName("Public job discovery needs no token")
    void publicEndpointsStayOpen() throws Exception {
        MvcResult result = mockMvc.perform(get(PUBLIC_PATH)).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("public jobs must be readable while signed out")
                .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("A request with no token is rejected before it reaches a handler")
    void anonymousIsRejectedOnProtectedPaths() throws Exception {
        mockMvc.perform(get(MODERATOR_PATH))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    /* ------------------------------------------------------------ helpers --- */

    private void assertAllowed(String path, String role) throws Exception {
        MvcResult result = perform(path, role);

        assertThat(result.getResponse().getStatus())
                .as("%s should be allowed to call %s", role, path)
                .isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    private void assertForbidden(String path, String role) throws Exception {
        MvcResult result = perform(path, role);

        assertThat(result.getResponse().getStatus())
                .as("%s must not be allowed to call %s", role, path)
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    /**
     * Authorities are supplied directly rather than as a `realm_access` claim:
     * the production converter's mapping is a separate concern, and these tests
     * are about what the rules do with the resulting `ROLE_*` authority.
     */
    private MvcResult perform(String path, String role) throws Exception {
        return mockMvc
                .perform(get(path).with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + role))))
                .andReturn();
    }
}
