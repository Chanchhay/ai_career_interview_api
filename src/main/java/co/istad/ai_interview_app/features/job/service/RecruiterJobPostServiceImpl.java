package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.job.dto.JobPostRequest;
import co.istad.ai_interview_app.features.job.dto.JobPostSectionRequest;
import co.istad.ai_interview_app.features.job.dto.JobPostSkillRequest;
import co.istad.ai_interview_app.features.job.dto.JobPostResponse;
import co.istad.ai_interview_app.features.job.entity.JobCategory;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.JobPostSection;
import co.istad.ai_interview_app.features.job.entity.JobPostSkill;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.job.mapper.JobPostMapper;
import co.istad.ai_interview_app.features.job.repository.JobCategoryRepository;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.features.job.repository.SkillRepository;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.recruiter.service.AuthenticatedRecruiterProfileResolver;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class RecruiterJobPostServiceImpl implements RecruiterJobPostService {

    private final JobPostRepository jobPostRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
    private final CompanyRepository companyRepository;
    private final AuthenticatedRecruiterProfileResolver recruiterProfileResolver;
    private final JobPostMapper jobPostMapper;

    @Override
    @Transactional
    public JobPostResponse createJobDraft(JobPostRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());

        RecruiterProfile recruiterProfile = recruiterProfileResolver.resolve();
        Company company = resolveMyCompany();
        JobPost jobPost = new JobPost();

        jobPost.setRecruiterProfile(recruiterProfile);
        jobPost.setCompany(company);
        applyRequest(jobPost, request);

        return jobPostMapper.toResponse(jobPostRepository.save(jobPost));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPostResponse> getMyJobs() {
        return jobPostRepository.findAllByRecruiterProfile_UserAccount_KeycloakUserIdOrderByCreatedAtDesc(AuthUtils.extractUserId())
                .stream()
                .map(jobPostMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostResponse getMyJob(Long id) {
        return jobPostMapper.toResponse(resolveMyJob(id));
    }

    @Override
    @Transactional
    public JobPostResponse updateMyJob(Long id, JobPostRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());

        JobPost jobPost = resolveMyJob(id);
        if (jobPost.getStatus() == JobStatus.CLOSED || jobPost.getStatus() == JobStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closed or expired jobs cannot be updated");
        }

        applyRequest(jobPost, request);

        return jobPostMapper.toResponse(jobPost);
    }

    @Override
    @Transactional
    public JobPostResponse publishMyJob(Long id) {
        JobPost jobPost = resolveMyJob(id);
        if (jobPost.getStatus() == JobStatus.CLOSED || jobPost.getStatus() == JobStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closed or expired jobs cannot be published");
        }

        jobPost.setStatus(JobStatus.PUBLISHED);
        if (jobPost.getPublishedAt() == null) {
            jobPost.setPublishedAt(Instant.now());
        }

        return jobPostMapper.toResponse(jobPost);
    }

    @Override
    @Transactional
    public JobPostResponse pauseMyJob(Long id) {
        JobPost jobPost = resolveMyJob(id);
        if (jobPost.getStatus() != JobStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only published jobs can be paused");
        }

        jobPost.setStatus(JobStatus.PAUSED);

        return jobPostMapper.toResponse(jobPost);
    }

    @Override
    @Transactional
    public JobPostResponse closeMyJob(Long id) {
        JobPost jobPost = resolveMyJob(id);
        if (jobPost.getStatus() == JobStatus.CLOSED) {
            return jobPostMapper.toResponse(jobPost);
        }

        jobPost.setStatus(JobStatus.CLOSED);

        return jobPostMapper.toResponse(jobPost);
    }

    private void applyRequest(JobPost jobPost, JobPostRequest request) {
        jobPost.setCategory(resolveCategory(request.categoryId()));
        jobPost.setTitle(normalizeBlankToNull(request.title()));
        jobPost.setDescription(normalizeBlankToNull(request.description()));
        jobPost.setLocation(normalizeBlankToNull(request.location()));
        jobPost.setJobType(normalizeBlankToNull(request.jobType()));
        jobPost.setWorkMode(normalizeBlankToNull(request.workMode()));
        jobPost.setSalaryMin(request.salaryMin());
        jobPost.setSalaryMax(request.salaryMax());
        jobPost.setExperienceLevel(normalizeBlankToNull(request.experienceLevel()));
        jobPost.setExpiredAt(request.expiredAt());

        replaceSections(jobPost, request.sections());
        replaceSkills(jobPost, request.skills());
    }

    private Company resolveMyCompany() {
        return companyRepository.findByRecruiterProfile_UserAccount_KeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Company profile was not found for authenticated recruiter"
                ));
    }

    private JobPost resolveMyJob(Long id) {
        return jobPostRepository.findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(id, AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job post was not found for authenticated recruiter"
                ));
    }

    private JobCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        return jobCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job category was not found"));
    }

    private void replaceSections(JobPost jobPost, List<JobPostSectionRequest> sectionRequests) {
        jobPost.getSections().clear();

        if (sectionRequests == null) {
            return;
        }

        sectionRequests.forEach(sectionRequest -> {
            JobPostSection section = new JobPostSection();
            section.setJobPost(jobPost);
            section.setSectionType(sectionRequest.sectionType());
            section.setTitle(normalizeBlankToNull(sectionRequest.title()));
            section.setContentMarkdown(normalizeBlankToNull(sectionRequest.contentMarkdown()));
            section.setContentText(normalizeBlankToNull(sectionRequest.contentText()));
            section.setDisplayOrder(sectionRequest.displayOrder());

            jobPost.getSections().add(section);
        });
    }

    private void replaceSkills(JobPost jobPost, List<JobPostSkillRequest> skillRequests) {
        if (skillRequests == null || skillRequests.isEmpty()) {
            jobPost.getSkills().clear();
            return;
        }

        Set<Long> skillIds = skillRequests.stream()
                .map(JobPostSkillRequest::skillId)
                .collect(Collectors.toCollection(HashSet::new));

        if (skillIds.size() != skillRequests.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate skills are not allowed");
        }

        Map<Long, Skill> skillsById = skillRepository.findAllById(skillIds)
                .stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));

        Set<Long> missingSkillIds = new HashSet<>(skillIds);
        missingSkillIds.removeAll(skillsById.keySet());
        if (!missingSkillIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more skills were not found");
        }

        Map<Long, JobPostSkill> existingLinksBySkillId = jobPost.getSkills()
                .stream()
                .collect(Collectors.toMap(jobPostSkill -> jobPostSkill.getSkill().getId(), Function.identity()));

        jobPost.getSkills().removeIf(jobPostSkill -> !skillIds.contains(jobPostSkill.getSkill().getId()));

        skillRequests.forEach(skillRequest -> {
            JobPostSkill jobPostSkill = existingLinksBySkillId.get(skillRequest.skillId());
            if (jobPostSkill == null) {
                jobPostSkill = new JobPostSkill();
                jobPostSkill.setJobPost(jobPost);
                jobPostSkill.setSkill(skillsById.get(skillRequest.skillId()));
                jobPost.getSkills().add(jobPostSkill);
            }
            jobPostSkill.setRequiredLevel(normalizeBlankToNull(skillRequest.requiredLevel()));
        });
    }

    private void validateSalaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum salary cannot be greater than maximum salary"
            );
        }
    }
}
