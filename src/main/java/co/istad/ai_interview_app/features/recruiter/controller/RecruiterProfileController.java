package co.istad.ai_interview_app.features.recruiter.controller;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.common.security.AuthenticatedUserResolver;
import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileResponse;
import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileUpdateRequest;
import co.istad.ai_interview_app.features.recruiter.service.RecruiterProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruiter/profile")
@RequiredArgsConstructor
public class RecruiterProfileController {

    private final RecruiterProfileService recruiterProfileService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @PutMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<RecruiterProfileResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody RecruiterProfileUpdateRequest request
    ) {
        return ApiResponse.success(
                recruiterProfileService.updateMyProfile(authenticatedUserResolver.resolveSubject(authentication), request)
        );
    }
}
