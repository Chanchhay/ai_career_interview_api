package co.istad.ai_interview_app.features.auth;

import co.istad.ai_interview_app.features.auth.dto.RegisterRequest;
import co.istad.ai_interview_app.features.auth.dto.RegisterResponse;
import co.istad.ai_interview_app.features.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }
}
