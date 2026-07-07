package co.istad.ai_interview_app.features.recruiter.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileResponse;
import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileUpdateRequest;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.recruiter.mapper.RecruiterProfileMapper;
import co.istad.ai_interview_app.features.recruiter.repository.RecruiterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecruiterProfileServiceImpl implements RecruiterProfileService {

    private final RecruiterProfileRepository recruiterProfileRepository;
    private final RecruiterProfileMapper recruiterProfileMapper;

    @Override
    @Transactional
    public RecruiterProfileResponse updateMyProfile(
            RecruiterProfileUpdateRequest request
    ) {
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserAccount_KeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Recruiter profile was not found for authenticated user"
                ));

        recruiterProfile.setPosition(normalize(request.position()));
        recruiterProfile.setLinkedinUrl(normalize(request.linkedinUrl()));

        return recruiterProfileMapper.toResponse(recruiterProfile);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}