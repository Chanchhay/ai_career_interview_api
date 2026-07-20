package co.istad.ai_interview_app.features.seeker.mapper;

import co.istad.ai_interview_app.features.seeker.dto.JobSeekerProfileResponse;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import org.springframework.stereotype.Component;

@Component
public class JobSeekerProfileMapper {

    public JobSeekerProfileResponse toResponse(JobSeekerProfile profile) {
        return new JobSeekerProfileResponse(
                profile.getId(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getCurrentPosition(),
                profile.getExpectedSalaryMin(),
                profile.getExpectedSalaryMax(),
                profile.getExpectedSalaryCurrency(),
                profile.getSalaryVisibility(),
                profile.getPreferredLocation(),
                profile.getAvailabilityStatus(),
                profile.getPublicProfileSlug(),
                profile.getProfileVisibility(),
                profile.getPublishedAt(),
                profile.getVerificationStatus(),
                profile.getStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
