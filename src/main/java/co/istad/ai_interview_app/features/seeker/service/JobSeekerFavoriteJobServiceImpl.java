package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.company.service.CompanyIdentity;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.features.seeker.dto.FavoriteJobResponse;
import co.istad.ai_interview_app.features.seeker.entity.FavoriteJob;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.repository.FavoriteJobRepository;
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

@Service
@RequiredArgsConstructor
public class JobSeekerFavoriteJobServiceImpl implements JobSeekerFavoriteJobService {

    private final FavoriteJobRepository favoriteJobRepository;
    private final JobPostRepository jobPostRepository;
    private final AuthenticatedJobSeekerProfileResolver jobSeekerProfileResolver;

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteJobResponse> findFavoriteJobs(Pageable pageable) {
        JobSeekerProfile profile = jobSeekerProfileResolver.resolve();

        return favoriteJobRepository
                .findPageByJobSeekerProfileId(profile.getId(), pageable)
                .map(this::toResponse);
    }

    /**
     * Idempotent: saving a job that is already saved returns the existing row
     * rather than failing. The bookmark button is the kind of control a user
     * double-taps, and a 409 there would be noise, not information.
     */
    @Override
    @Transactional
    public FavoriteJobResponse saveFavoriteJob(Long jobId) {
        JobSeekerProfile profile = jobSeekerProfileResolver.resolve();

        return favoriteJobRepository
                .findByJobSeekerProfile_IdAndJobPost_Id(profile.getId(), jobId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    FavoriteJob favoriteJob = new FavoriteJob();
                    favoriteJob.setJobSeekerProfile(profile);
                    favoriteJob.setJobPost(resolveSavableJob(jobId));

                    return toResponse(favoriteJobRepository.save(favoriteJob));
                });
    }

    @Override
    @Transactional
    public void removeFavoriteJob(Long jobId) {
        JobSeekerProfile profile = jobSeekerProfileResolver.resolve();

        favoriteJobRepository
                .findByJobSeekerProfile_IdAndJobPost_Id(profile.getId(), jobId)
                .ifPresentOrElse(
                        favoriteJobRepository::delete,
                        () -> {
                            throw new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Saved job was not found"
                            );
                        }
                );
    }

    /**
     * Only a job the seeker could have discovered may be saved — published, by
     * an approved and active company, not past its expiry. Resolved through the
     * same query the public detail page uses, so a draft or a paused post
     * cannot be bookmarked by guessing its id.
     */
    private JobPost resolveSavableJob(Long jobId) {
        return jobPostRepository.findPublicJobById(
                        jobId,
                        JobStatus.PUBLISHED,
                        VerificationStatus.APPROVED,
                        ProfileStatus.ACTIVE,
                        Instant.now()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Public job was not found"
                ));
    }

    private FavoriteJobResponse toResponse(FavoriteJob favoriteJob) {
        JobPost jobPost = favoriteJob.getJobPost();

        return new FavoriteJobResponse(
                favoriteJob.getId(),
                favoriteJob.getCreatedAt(),
                jobPost.getId(),
                jobPost.getTitle(),
                CompanyIdentity.displayId(jobPost.getCompany()),
                CompanyIdentity.displayName(jobPost.getCompany()),
                jobPost.getLocation(),
                jobPost.getJobType(),
                jobPost.getWorkMode(),
                jobPost.getSalaryMin(),
                jobPost.getSalaryMax(),
                jobPost.getExperienceLevel(),
                jobPost.getStatus(),
                jobPost.getPublishedAt(),
                jobPost.getExpiredAt(),
                isStillOpen(jobPost)
        );
    }

    private boolean isStillOpen(JobPost jobPost) {
        return jobPost.getStatus() == JobStatus.PUBLISHED
                && (jobPost.getExpiredAt() == null || jobPost.getExpiredAt().isAfter(Instant.now()));
    }
}
