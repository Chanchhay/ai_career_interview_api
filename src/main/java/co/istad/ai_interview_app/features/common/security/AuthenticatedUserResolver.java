package co.istad.ai_interview_app.features.common.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticatedUserResolver {

    public String resolveSubject(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Jwt jwt = jwtAuthentication.getToken();
            String subject = jwt.getSubject();

            if (subject != null && !subject.isBlank()) {
                return subject;
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated token is missing subject");
    }
}
