package co.istad.ai_interview_app.config.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AuthUtils {

    private AuthUtils() {
    }

    public static String extractUserId() {
        return extractRequiredSubject(extractJwtToken());
    }

    public static Optional<String> extractUserIdIfAuthenticated() {
        Authentication auth = getAuth();

        if (auth instanceof JwtAuthenticationToken jwtAuthentication) {
            String subject = jwtAuthentication.getToken().getSubject();
            return hasText(subject) ? Optional.of(subject) : Optional.empty();
        }

        return Optional.empty();
    }

    public static Jwt extractJwtToken() {
        return extractJwtAuthentication().getToken();
    }

    public static String extractJwt() {
        return extractJwtToken().getTokenValue();
    }

    public static JwtAuthenticationToken extractJwtAuthentication() {
        Authentication auth = getAuth();

        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have been unauthorized");
        }

        if (auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have been forbidden");
        }

        if (auth instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated request is not JWT-based");
    }

    public static Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String extractRequiredSubject(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated token is missing");
        }

        String subject = jwt.getSubject();

        if (!hasText(subject)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated token is missing subject");
        }

        return subject;
    }

    public static String claimAsString(Jwt jwt, String claimName) {
        if (jwt == null) {
            return null;
        }

        Object value = jwt.getClaims().get(claimName);
        return value == null ? null : value.toString();
    }

    public static String resolveFullName(Jwt jwt) {
        String fullName = claimAsString(jwt, "name");

        if (hasText(fullName)) {
            return fullName;
        }

        String givenName = claimAsString(jwt, "given_name");
        String familyName = claimAsString(jwt, "family_name");
        String combinedName = Stream.of(givenName, familyName)
                .filter(AuthUtils::hasText)
                .collect(Collectors.joining(" "));

        if (hasText(combinedName)) {
            return combinedName;
        }

        String preferredUsername = claimAsString(jwt, "preferred_username");
        if (hasText(preferredUsername)) {
            return preferredUsername;
        }

        return claimAsString(jwt, "email");
    }

    public static boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
        String roleAuthority = "ROLE_" + role;
        return streamAuthorities(authorities)
                .anyMatch(authority -> authority.equalsIgnoreCase(roleAuthority) || authority.equalsIgnoreCase(role));
    }

    public static List<String> resolveRoles(Collection<? extends GrantedAuthority> authorities) {
        return streamAuthorities(authorities)
                .sorted()
                .collect(Collectors.toList());
    }

    private static Stream<String> streamAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return Stream.empty();
        }

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
