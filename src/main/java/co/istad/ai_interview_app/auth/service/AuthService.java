package co.istad.ai_interview_app.auth.service;

import co.istad.ai_interview_app.auth.dto.RegisterRequest;
import co.istad.ai_interview_app.auth.dto.RegisterResponse;
import jakarta.validation.Valid;

public interface AuthService {
    RegisterResponse register(@Valid RegisterRequest registerRequest);
}
