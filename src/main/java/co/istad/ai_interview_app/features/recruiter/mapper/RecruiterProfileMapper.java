package co.istad.ai_interview_app.features.recruiter.mapper;

import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileResponse;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import org.springframework.stereotype.Component;

@Component
public class RecruiterProfileMapper {

    public RecruiterProfileResponse toResponse(RecruiterProfile profile) {
        return new RecruiterProfileResponse(
                profile.getId(),
                profile.getPosition(),
                profile.getLinkedinUrl(),
                profile.getStatus()
        );
    }
}
