package co.istad.ai_interview_app.features.recruiter.mapper;

import co.istad.ai_interview_app.features.recruiter.dto.RecruiterProfileResponse;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecruiterProfileMapper {

    RecruiterProfileResponse toResponse(RecruiterProfile profile);
}
