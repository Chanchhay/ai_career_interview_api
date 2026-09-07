package co.istad.ai_interview_app.features.job.dto;

import co.istad.ai_interview_app.shared.enums.job.JobPostSectionType;
import java.util.UUID;

public record JobPostSectionResponse(
        UUID id,
        JobPostSectionType sectionType,
        String title,
        String contentMarkdown,
        String contentText,
        Integer displayOrder
) {
}
