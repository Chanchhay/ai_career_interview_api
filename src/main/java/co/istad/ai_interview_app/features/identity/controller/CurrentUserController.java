package co.istad.ai_interview_app.features.identity.controller;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.identity.dto.CurrentUserResponse;
import co.istad.ai_interview_app.features.identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurrentUserController {

    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser() {
        return ApiResponse.success(currentUserService.getCurrentUser());
    }
}
