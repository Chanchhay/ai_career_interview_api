package co.istad.ai_interview_app.features.job.mapper;

import co.istad.ai_interview_app.features.job.dto.JobPostResponse;
import co.istad.ai_interview_app.features.job.dto.JobPostSectionResponse;
import co.istad.ai_interview_app.features.job.dto.JobPostSkillResponse;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.JobPostSection;
import co.istad.ai_interview_app.features.job.entity.JobPostSkill;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class JobPostMapper {

    public JobPostResponse toResponse(JobPost jobPost) {
        return new JobPostResponse(
                jobPost.getId(),
                jobPost.getCompany().getId(),
                jobPost.getCompany().getName(),
                jobPost.getRecruiterProfile().getId(),
                jobPost.getCategory() == null ? null : jobPost.getCategory().getId(),
                jobPost.getCategory() == null ? null : jobPost.getCategory().getName(),
                jobPost.getTitle(),
                jobPost.getDescription(),
                jobPost.getLocation(),
                jobPost.getJobType(),
                jobPost.getWorkMode(),
                jobPost.getSalaryMin(),
                jobPost.getSalaryMax(),
                jobPost.getExperienceLevel(),
                jobPost.getStatus(),
                jobPost.getPublishedAt(),
                jobPost.getExpiredAt(),
                toSectionResponses(jobPost.getSections()),
                toSkillResponses(jobPost.getSkills())
        );
    }

    private List<JobPostSectionResponse> toSectionResponses(List<JobPostSection> sections) {
        return sections.stream()
                .sorted(Comparator.comparing(JobPostSection::getDisplayOrder))
                .map(section -> new JobPostSectionResponse(
                        section.getId(),
                        section.getSectionType(),
                        section.getTitle(),
                        section.getContentMarkdown(),
                        section.getContentText(),
                        section.getDisplayOrder()
                ))
                .toList();
    }

    private List<JobPostSkillResponse> toSkillResponses(List<JobPostSkill> skills) {
        return skills.stream()
                .sorted(Comparator.comparing(jobPostSkill -> jobPostSkill.getSkill().getName()))
                .map(jobPostSkill -> new JobPostSkillResponse(
                        jobPostSkill.getId(),
                        jobPostSkill.getSkill().getId(),
                        jobPostSkill.getSkill().getName(),
                        jobPostSkill.getSkill().getSkillType(),
                        jobPostSkill.getRequiredLevel()
                ))
                .toList();
    }
}
