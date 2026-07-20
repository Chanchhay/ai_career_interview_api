package co.istad.ai_interview_app.features.auth.service;

import co.istad.ai_interview_app.features.auth.dto.RegisterRequest;
import co.istad.ai_interview_app.features.auth.dto.RegisterResponse;
import jakarta.validation.Valid;

public interface AuthService {
    RegisterResponse register(@Valid RegisterRequest registerRequest);
}
