package co.istad.ai_interview_app.features.identity.service;

import co.istad.ai_interview_app.features.identity.dto.CurrentUserResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public interface CurrentUserService {
    CurrentUserResponse getCurrentUser(
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities
    );
}
