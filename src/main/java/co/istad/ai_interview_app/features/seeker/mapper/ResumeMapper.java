package co.istad.ai_interview_app.features.seeker.mapper;

import co.istad.ai_interview_app.features.seeker.dto.ResumeResponse;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {

    public ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getTitle(),
                resume.getResumeFileUrl(),
                resume.getResumeData(),
                resume.getIsDefault(),
                resume.getVisibility(),
                resume.getPublishedAt(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
