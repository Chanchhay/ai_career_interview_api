package co.istad.ai_interview_app.features.recruiter;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileResponse;
import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileUpdateRequest;
import co.istad.ai_interview_app.features.recruiter.service.RecruiterProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiter/profile")
@RequiredArgsConstructor
public class RecruiterProfileController {

    private final RecruiterProfileService recruiterProfileService;

    @GetMapping
    public ApiResponse<RecruiterProfileResponse> getMyProfile() {
        return ApiResponse.success(
                recruiterProfileService.getMyProfile()
        );
    }

    @PatchMapping
    public ApiResponse<RecruiterProfileResponse> updateMyProfile(
            @Valid @RequestBody RecruiterProfileUpdateRequest request
    ) {
        return ApiResponse.success(
                recruiterProfileService.updateMyProfile(request)
        );
    }
}
