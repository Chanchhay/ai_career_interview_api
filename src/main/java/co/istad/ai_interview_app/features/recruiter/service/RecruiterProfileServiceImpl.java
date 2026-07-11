package co.istad.ai_interview_app.features.recruiter.service;

import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileResponse;
import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileUpdateRequest;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.recruiter.mapper.RecruiterProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class RecruiterProfileServiceImpl implements RecruiterProfileService {

    private final AuthenticatedRecruiterProfileResolver recruiterProfileResolver;
    private final RecruiterProfileMapper recruiterProfileMapper;

    @Override
    @Transactional
    public RecruiterProfileResponse updateMyProfile(
            RecruiterProfileUpdateRequest request
    ) {
        RecruiterProfile recruiterProfile = recruiterProfileResolver.resolve();

        recruiterProfile.setPosition(normalizeBlankToNull(request.position()));
        recruiterProfile.setLinkedinUrl(normalizeBlankToNull(request.linkedinUrl()));

        return recruiterProfileMapper.toResponse(recruiterProfile);
    }
}
