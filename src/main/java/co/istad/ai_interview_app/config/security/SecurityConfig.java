package co.istad.ai_interview_app.config.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

/**
 * Every authorization rule in the application lives here, expressed as URL
 * patterns. Controllers carry no {@code @PreAuthorize}: one list that can be
 * read top to bottom is easier to audit than sixty annotations spread across
 * thirteen files, and it cannot drift from the paths it guards.
 *
 * <p>Roles come from the Keycloak realm and must be spelled exactly as they are
 * there — the JWT converter below turns {@code realm_access.roles} into
 * {@code ROLE_<name>} authorities, which is what {@code hasRole} matches.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Platform owner. Reaches every staff area through {@link #roleHierarchy()}. */
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    /** Verifies companies, reviews candidates, curates the shared taxonomy. */
    private static final String MODERATOR = "MODERATOR";

    /** Billing and revenue. No endpoints yet; the rule is here for when there are. */
    private static final String FINANCE = "FINANCE";

    private static final String SEEKER = "SEEKER";

    /**
     * Accepted alongside {@link #SEEKER} because accounts were issued under both
     * spellings. Drop it once the realm has been reconciled on SEEKER.
     */
    private static final String SEEKER_LEGACY = "JOB_SEEKER";

    private static final String RECRUITER = "RECRUITER";

    /**
     * The one place SUPER_ADMIN's reach is defined: it holds every staff role
     * implicitly, so {@code hasRole(MODERATOR)} below admits it without naming
     * it.
     *
     * <p>Deliberately not extended to SEEKER or RECRUITER. Those endpoints
     * resolve the caller's own seeker or recruiter profile, so admitting an
     * account that has neither would trade a clean 403 for a confusing 404 from
     * deeper in the service. Both prefixes name SUPER_ADMIN explicitly instead,
     * which keeps that decision visible at the rule.
     *
     * <p>Spring Security picks this bean up on its own: the authorize-requests
     * configurer builds its authorization manager factory with whatever
     * {@code RoleHierarchy} bean the context holds.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(SUPER_ADMIN).implies(MODERATOR, FINANCE)
                .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            IdentityUserAccountRepository userAccountRepository
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        /* ---------------------------------------- open --- */

                        .requestMatchers("/actuator/health").permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register"
                        ).permitAll()

                        // Job discovery and published profiles: readable signed
                        // out, which is what lets the marketing site work.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/jobs/public/**",
                                "/api/v1/public/**"
                        ).permitAll()

                        // Guest interviews are the one thing a signed-out
                        // visitor may write. Scoped to this prefix and these two
                        // methods rather than opening /api/v1/public/** to all
                        // verbs: everything else under it is readable on purpose
                        // and must stay read-only. The session itself is
                        // protected by the guest's own token, which the service
                        // requires on every one of these calls.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/public/guest-interviews/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/public/guest-interviews/**"
                        ).permitAll()

                        // Vapi's voice-interview webhook. Open to the filter
                        // chain because the caller is Vapi, which holds no
                        // Keycloak token and must never be issued one. It is not
                        // unauthenticated: the handler rejects any request whose
                        // shared secret does not match before acting on the body,
                        // and fails closed when no secret is configured. Kept off
                        // /api/v1/job-seeker/** on purpose so this exception
                        // cannot widen the seeker rules.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/integrations/vapi/webhook"
                        ).permitAll()

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/scalar/**"
                        ).permitAll()

                        /* ------------------------- any signed-in account --- */

                        // Which file a given caller may read is the storage
                        // service's decision, not a question a URL pattern can
                        // answer.
                        // Identity, file upload plus private download, and a
                        // person's own notification inbox. Notifications are
                        // not role-scoped: every signed-in account has one, and
                        // the service filters every query to the caller.
                        // Conversations are membership-scoped, not role-scoped:
                        // a thread's two sides hold different roles and both
                        // need the same reads and replies, so the service checks
                        // participation instead. Opening a thread is not here —
                        // that lives under /api/v1/moderator/conversations.
                        .requestMatchers(
                                "/api/v1/me",
                                "/api/v1/files/**",
                                "/api/v1/notifications/**",
                                "/api/v1/conversations/**"
                        ).authenticated()

                        /* ------------------------------------ role areas --- */

                        // SUPER_ADMIN is named here rather than inherited: see
                        // the note on roleHierarchy().
                        .requestMatchers("/api/v1/job-seeker/**")
                        .hasAnyRole(SEEKER, SEEKER_LEGACY, SUPER_ADMIN)

                        .requestMatchers("/api/v1/recruiter/**")
                        .hasAnyRole(RECRUITER, SUPER_ADMIN)

                        // The next three admit SUPER_ADMIN through the hierarchy.
                        .requestMatchers("/api/v1/moderator/**").hasRole(MODERATOR)

                        // Ahead of the line below on purpose. Account
                        // management grants roles and suspends people, so it is
                        // SUPER_ADMIN's alone — a moderator who reached it could
                        // grant themselves SUPER_ADMIN.
                        .requestMatchers("/api/v1/admin/users/**").hasRole(SUPER_ADMIN)

                        // The AI engine: model, provider API key, tuning. Held
                        // to SUPER_ADMIN rather than the MODERATOR rule below,
                        // because the key stored there spends real money and a
                        // wrong model takes every AI feature down at once.
                        .requestMatchers("/api/v1/admin/ai-provider-config/**")
                        .hasRole(SUPER_ADMIN)

                        // Shared taxonomy — industries, job categories, skills —
                        // and the AI interview question mix. Moderators curate it
                        // while reviewing, so it sits with the moderator rules
                        // despite the /admin path.
                        .requestMatchers("/api/v1/admin/**").hasRole(MODERATOR)

                        .requestMatchers("/api/v1/finance/**").hasRole(FINANCE)

                        /* --------------------------------------- default --- */

                        // Anything not matched above still needs a token, so a
                        // new controller fails closed rather than open.
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Straight after the token is turned into an Authentication, so
                // a suspended account is stopped before any authorization rule
                // above gets to admit it.
                .addFilterAfter(
                        new AccountStatusFilter(userAccountRepository),
                        BearerTokenAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${app.security.jwt.connect-timeout:PT5S}") Duration connectTimeout,
            @Value("${app.security.jwt.read-timeout:PT30S}") Duration readTimeout
    ) {
        String resolvedJwkSetUri = hasText(jwkSetUri)
                ? jwkSetUri
                : issuerUri + "/protocol/openid-connect/certs";

        log.info(
                "Configuring JWT resource server with issuer={} jwkSetUri={}",
                issuerUri,
                resolvedJwkSetUri
        );

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(resolvedJwkSetUri)
                .restOperations(new RestTemplate(requestFactory))
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .jwsAlgorithm(SignatureAlgorithm.RS384)
                .jwsAlgorithm(SignatureAlgorithm.RS512)
                .build();

        OAuth2TokenValidator<Jwt> validator =
                JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(validator);

        return decoder;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint delegate =
                new BearerTokenAuthenticationEntryPoint();

        return (request, response, authException) -> {
            logAuthenticationFailure(request, authException);
            delegate.commence(request, response, authException);
        };
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        BearerTokenAccessDeniedHandler delegate =
                new BearerTokenAccessDeniedHandler();

        return (request, response, accessDeniedException) -> {
            logAccessDenied(request, accessDeniedException);
            delegate.handle(request, response, accessDeniedException);
        };
    }

    private void logAuthenticationFailure(
            HttpServletRequest request,
            AuthenticationException exception
    ) {
        log.warn(
                "Authentication failed for {} {}: {} - {}; authorizationHeaderPresent={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                hasText(request.getHeader("Authorization"))
        );
    }

    private void logAccessDenied(
            HttpServletRequest request,
            AccessDeniedException exception
    ) {
        log.warn(
                "Access denied for {} {}: {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Map<String, Object> realmAccess =
                    jwt.getClaimAsMap("realm_access");

            if (realmAccess != null
                    && realmAccess.get("roles") instanceof Collection<?> roles) {


                roles.stream()
                        .map(String::valueOf)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }

            log.debug(
                    "Authenticated subject={}, username={}, authorities={}",
                    jwt.getSubject(),
                    jwt.getClaimAsString("preferred_username"),
                    authorities
            );

            return authorities;
        });

        return converter;
    }
}
