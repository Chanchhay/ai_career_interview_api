package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.seeker.dto.JobSeekerProfileResponse;
import co.istad.ai_interview_app.features.seeker.dto.JobSeekerProfileUpdateRequest;
import co.istad.ai_interview_app.features.seeker.service.JobSeekerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/job-seeker/profile")
@RequiredArgsConstructor
public class JobSeekerProfileController {

    private final JobSeekerProfileService profileService;

    @GetMapping
    public ApiResponse<JobSeekerProfileResponse> getMyProfile() {
        return ApiResponse.success(profileService.getMyProfile());
    }

    @PatchMapping
    public ApiResponse<JobSeekerProfileResponse> updateMyProfile(
            @Valid @RequestBody JobSeekerProfileUpdateRequest request
    ) {
        return ApiResponse.success(profileService.updateMyProfile(request));
    }
}
