package co.istad.ai_interview_app.config.security;

import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.shared.enums.account.AccountStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Rejects requests from an account that is no longer active.
 *
 * <p>Suspension disables the Keycloak user, which stops new logins — but an
 * access token issued a minute earlier stays cryptographically valid until it
 * expires, and the resource server would happily accept it. This filter closes
 * that window: the local {@code user_accounts.status} is consulted on every
 * authenticated request, so a suspension takes effect on the suspended user's
 * very next call rather than whenever their token happens to run out.
 *
 * <p>Costs one indexed lookup per authenticated request. That is deliberate —
 * caching the status would reopen the same staleness window this filter exists
 * to close, only shorter. If it ever shows up in a profile, the fix is a cache
 * with explicit invalidation on suspend, not a longer TTL.
 *
 * <p>A subject with no local {@code user_accounts} row is allowed through. Such
 * an account has nothing to suspend, and failing closed here would lock out a
 * SUPER_ADMIN created directly in Keycloak — including the one who would have
 * to undo it.
 */
/*
 * Deliberately not a @Component. Spring Boot auto-registers any Filter bean
 * with the servlet container as well, which would run this a second time
 * outside the security chain — before authentication exists, where it can only
 * waste a call. SecurityConfig constructs it and adds it at one explicit point.
 */
@Slf4j
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {

    private final IdentityUserAccountRepository userAccountRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        String keycloakUserId = jwtAuthentication.getToken().getSubject();

        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<AccountStatus> status = userAccountRepository.findStatusByKeycloakUserId(keycloakUserId);

        if (status.isPresent() && status.get() != AccountStatus.ACTIVE) {
            log.warn(
                    "Rejected {} {} from account {} with status {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    keycloakUserId,
                    status.get()
            );

            SecurityContextHolder.clearContext();
            writeForbidden(response, status.get());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response, AccountStatus status) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"message\":\"This account is %s and cannot access the platform\",\"data\":null}"
                        .formatted(status.name().toLowerCase())
        );
    }
}
