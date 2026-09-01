package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.features.company.repository.IndustryRepository;
import co.istad.ai_interview_app.features.job.dto.JobPostSectionResponse;
import co.istad.ai_interview_app.features.job.dto.JobPostSkillResponse;
import co.istad.ai_interview_app.features.job.dto.PublicIndustryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobFacetsResponse;
import co.istad.ai_interview_app.features.job.dto.PublicJobFacetsResponse.PublicJobFacetValue;
import co.istad.ai_interview_app.features.job.dto.PublicJobFilter;
import co.istad.ai_interview_app.features.company.service.CompanyIdentity;
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
import co.istad.ai_interview_app.features.seeker.repository.FavoriteJobRepository;
import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.util.TextUtils;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import co.istad.ai_interview_app.features.job.specification.JobPostSpecification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class PublicJobServiceImpl implements PublicJobService {

    /** The windows the "date posted" filter offers, in days. */
    private static final List<Integer> POSTED_WITHIN_DAYS = List.of(1, 7, 30);

    private final JobPostRepository jobPostRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
    private final IndustryRepository industryRepository;
    private final FavoriteJobRepository favoriteJobRepository;
    private final PublicJobFacetCounter facetCounter;

    @Override
    @Transactional(readOnly = true)
    public Page<PublicJobResponse> findPublicJobs(PublicJobFilter filter, Pageable pageable) {
        Specification<JobPost> spec = spec(normalize(filter))
                .and(JobPostSpecification.orderedBy(pageable.getSort()));

        Page<JobPost> page = jobPostRepository.findAll(spec, withoutSort(pageable));
        Set<Long> savedJobIds = resolveSavedJobIds(page.map(JobPost::getId).getContent());

        return page.map(jobPost -> toPublicResponse(jobPost, savedJobIds));
    }

    /**
     * Trims the filter down to what the specification can act on: blank strings
     * become null, and repeated or empty list entries are dropped, so that
     * {@code ?jobType=&jobType=FULL_TIME} does not silently exclude every job.
     */
    private PublicJobFilter normalize(PublicJobFilter filter) {
        return new PublicJobFilter(
                normalizeBlankToNull(filter.keyword()),
                normalizeBlankToNull(filter.location()),
                distinctValues(filter.categoryIds()),
                distinctValues(filter.skillIds()),
                distinctText(filter.workModes()),
                distinctText(filter.jobTypes()),
                distinctText(filter.experienceLevels()),
                filter.salaryMin(),
                filter.salaryMax(),
                filter.postedAfter()
        );
    }

    private <T> List<T> distinctValues(List<T> values) {
        return values == null
                ? List.of()
                : values.stream().filter(Objects::nonNull).distinct().toList();
    }

    private List<String> distinctText(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                .map(TextUtils::normalizeBlankToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicJobFacetsResponse findPublicJobFacets(PublicJobFilter filter) {
        PublicJobFilter normalized = normalize(filter);

        return new PublicJobFacetsResponse(
                facetCounter.countByColumn(spec(normalized.withoutJobTypes()), "jobType"),
                facetCounter.countByColumn(spec(normalized.withoutWorkModes()), "workMode"),
                facetCounter.countByColumn(spec(normalized.withoutExperienceLevels()), "experienceLevel"),
                facetCounter.countByCategory(spec(normalized.withoutCategories())),
                facetCounter.countBySkill(spec(normalized.withoutSkills())),
                countPostedWithin(normalized),
                facetCounter.salaryBounds(spec(normalized.withoutSalary())),
                facetCounter.count(spec(normalized))
        );
    }

    /**
     * How many of the matching jobs went up in the last day, week and month.
     * Counted against the filter with its own window lifted, like every other
     * group, so switching from "last week" to "last month" stays possible.
     */
    private List<PublicJobFacetValue> countPostedWithin(PublicJobFilter filter) {
        Instant now = Instant.now();

        return POSTED_WITHIN_DAYS.stream()
                .map(days -> new PublicJobFacetValue(
                        String.valueOf(days),
                        facetCounter.count(spec(filter.withPostedAfter(now.minus(days, ChronoUnit.DAYS))))
                ))
                .filter(facet -> facet.count() > 0)
                .toList();
    }

    /** The caller's filter under the visibility rules no request can widen. */
    private Specification<JobPost> spec(PublicJobFilter filter) {
        return JobPostSpecification.filterPublicJobs(
                JobStatus.PUBLISHED,
                VerificationStatus.APPROVED,
                ProfileStatus.ACTIVE,
                Instant.now(),
                filter
        );
    }

    /**
     * Hands the page window over without its sort, because
     * {@link JobPostSpecification#orderedBy} has already put the ordering on
     * the query. Spring Data would otherwise overwrite it with its own.
     */
    private Pageable withoutSort(Pageable pageable) {
        return pageable.isUnpaged()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
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
                .map(jobPost -> toPublicResponse(jobPost, resolveSavedJobIds(List.of(jobPost.getId()))))
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

    /**
     * Which of {@code jobPostIds} the caller has bookmarked, as one query for
     * the whole page rather than one per row.
     *
     * <p>Returns {@code null} — not an empty set — when nobody is signed in, so
     * {@link #toPublicResponse} can tell "not saved" apart from "no answer to
     * give". These endpoints are {@code permitAll}, so a token is optional
     * here; a recruiter or admin holds one but owns no favorites and matches
     * nothing.
     */
    private Set<Long> resolveSavedJobIds(List<Long> jobPostIds) {
        Optional<String> keycloakUserId = AuthUtils.extractUserIdIfAuthenticated();

        if (keycloakUserId.isEmpty()) {
            return null;
        }

        if (jobPostIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(favoriteJobRepository.findSavedJobPostIds(keycloakUserId.get(), jobPostIds));
    }

    private PublicJobResponse toPublicResponse(JobPost jobPost, Set<Long> savedJobIds) {
        return new PublicJobResponse(
                jobPost.getId(),
                CompanyIdentity.displayId(jobPost.getCompany()),
                CompanyIdentity.displayName(jobPost.getCompany()),
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
                toSkillResponses(jobPost.getSkills()),
                savedJobIds == null ? null : savedJobIds.contains(jobPost.getId())
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
