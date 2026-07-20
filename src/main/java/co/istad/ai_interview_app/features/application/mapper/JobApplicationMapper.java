package co.istad.ai_interview_app.features.application.mapper;

import co.istad.ai_interview_app.features.application.dto.JobApplicationResponse;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class JobApplicationMapper {

    public JobApplicationResponse toResponse(JobApplication application) {
        Resume resume = application.getResume();
        return new JobApplicationResponse(
                application.getId(),
                application.getJobPost().getId(),
                application.getJobPost().getTitle(),
                resume == null ? null : resume.getId(),
                resume == null ? null : resume.getTitle(),
                application.getCoverLetter(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getCreatedAt()
        );
    }
}
