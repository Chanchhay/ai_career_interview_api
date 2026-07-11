package co.istad.ai_interview_app.features.job.dto;

public record JobPostSkillResponse(
        Long id,
        Long skillId,
        String skillName,
        String skillType,
        String requiredLevel
) {
}
