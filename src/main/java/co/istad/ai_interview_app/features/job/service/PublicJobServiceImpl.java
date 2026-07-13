package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.features.company.repository.IndustryRepository;
import co.istad.ai_interview_app.features.job.dto.JobPostSectionResponse;
import co.istad.ai_interview_app.features.job.dto.JobPostSkillResponse;
import co.istad.ai_interview_app.features.job.dto.PublicIndustryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobResponse;
import co.istad.ai_interview_app.features.job.dto.PublicSkillResponse;
import co.istad.ai_interview_app.features.job.entity.JobCategory;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.JobPostSection;
import co.istad.ai_interview_app.features.job.entity.JobPostSkill;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.job.repository.JobCategoryRepository;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.features.job.repository.SkillRepository;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class PublicJobServiceImpl implements PublicJobService {

    private final JobPostRepository jobPostRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
    private final IndustryRepository industryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PublicJobResponse> findPublicJobs(
            String keyword,
            String location,
            Long categoryId,
            List<Long> skillIds,
            String workMode,
            String jobType,
            Pageable pageable
    ) {
        List<Long> normalizedSkillIds = skillIds == null ? List.of() : skillIds.stream().distinct().toList();
        List<Long> querySkillIds = normalizedSkillIds.isEmpty() ? List.of(-1L) : normalizedSkillIds;

        return jobPostRepository.findPublicJobs(
                        JobStatus.PUBLISHED,
                        VerificationStatus.APPROVED,
                        ProfileStatus.ACTIVE,
                        Instant.now(),
                        normalizeBlankToNull(keyword),
                        normalizeBlankToNull(location),
                        categoryId,
                        querySkillIds,
                        normalizedSkillIds.size(),
                        normalizeBlankToNull(workMode),
                        normalizeBlankToNull(jobType),
                        pageable
                )
                .map(this::toPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicJobResponse getPublicJob(Long jobId) {
        return jobPostRepository.findPublicJobById(
                        jobId,
                        JobStatus.PUBLISHED,
                        VerificationStatus.APPROVED,
                        ProfileStatus.ACTIVE,
                        Instant.now()
                )
                .map(this::toPublicResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public job was not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicJobCategoryResponse> getJobCategories() {
        return jobCategoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicSkillResponse> getSkills() {
        return skillRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toSkillResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicIndustryResponse> getIndustries() {
        return industryRepository.findAllByStatusOrderByNameAsc(ProfileStatus.ACTIVE)
                .stream()
                .map(this::toIndustryResponse)
                .toList();
    }

    private PublicJobResponse toPublicResponse(JobPost jobPost) {
        return new PublicJobResponse(
                jobPost.getId(),
                jobPost.getCompany().getId(),
                jobPost.getCompany().getName(),
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

    private PublicJobCategoryResponse toCategoryResponse(JobCategory category) {
        return new PublicJobCategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

    private PublicSkillResponse toSkillResponse(Skill skill) {
        return new PublicSkillResponse(skill.getId(), skill.getName(), skill.getSkillType());
    }

    private PublicIndustryResponse toIndustryResponse(Industry industry) {
        return new PublicIndustryResponse(industry.getId(), industry.getName(), industry.getDescription());
    }
}
