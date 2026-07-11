package co.istad.ai_interview_app.features.job.dto;

import co.istad.ai_interview_app.shared.enums.job.JobPostSectionType;

public record JobPostSectionResponse(
        Long id,
        JobPostSectionType sectionType,
        String title,
        String contentMarkdown,
        String contentText,
        Integer displayOrder
) {
}
