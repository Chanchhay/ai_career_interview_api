package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.JobSeekerProfileResponse;
import co.istad.ai_interview_app.features.seeker.dto.JobSeekerProfileUpdateRequest;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.mapper.JobSeekerProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class JobSeekerProfileServiceImpl implements JobSeekerProfileService {

    private final AuthenticatedJobSeekerProfileResolver profileResolver;
    private final JobSeekerProfileMapper profileMapper;

    @Override
    @Transactional(readOnly = true)
    public JobSeekerProfileResponse getMyProfile() {
        return profileMapper.toResponse(profileResolver.resolve());
    }

    @Override
    @Transactional
    public JobSeekerProfileResponse updateMyProfile(JobSeekerProfileUpdateRequest request) {
        JobSeekerProfile profile = profileResolver.resolve();

        if (request.headline() != null) {
            profile.setHeadline(normalizeBlankToNull(request.headline()));
        }
        if (request.bio() != null) {
            profile.setBio(normalizeBlankToNull(request.bio()));
        }
        if (request.currentPosition() != null) {
            profile.setCurrentPosition(normalizeBlankToNull(request.currentPosition()));
        }
        if (request.expectedSalaryMin() != null) {
            profile.setExpectedSalaryMin(request.expectedSalaryMin());
        }
        if (request.expectedSalaryMax() != null) {
            profile.setExpectedSalaryMax(request.expectedSalaryMax());
        }
        if (request.expectedSalaryCurrency() != null) {
            profile.setExpectedSalaryCurrency(normalizeBlankToNull(request.expectedSalaryCurrency()));
        }
        if (request.salaryVisibility() != null) {
            profile.setSalaryVisibility(request.salaryVisibility());
        }
        if (request.preferredLocation() != null) {
            profile.setPreferredLocation(normalizeBlankToNull(request.preferredLocation()));
        }
        if (request.availabilityStatus() != null) {
            profile.setAvailabilityStatus(normalizeBlankToNull(request.availabilityStatus()));
        }

        return profileMapper.toResponse(profile);
    }
}
