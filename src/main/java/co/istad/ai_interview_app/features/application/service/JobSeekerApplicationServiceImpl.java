package co.istad.ai_interview_app.features.application.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.application.dto.JobApplicationCreateRequest;
import co.istad.ai_interview_app.features.application.dto.JobApplicationResponse;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.application.mapper.JobApplicationMapper;
import co.istad.ai_interview_app.features.application.repository.JobApplicationRepository;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.features.seeker.repository.ResumeRepository;
import co.istad.ai_interview_app.features.seeker.service.AuthenticatedJobSeekerProfileResolver;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import org.springframework.context.ApplicationEventPublisher;
import co.istad.ai_interview_app.features.notification.event.NotificationEvents;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewSessionRepository;
import co.istad.ai_interview_app.features.moderator.entity.CandidateApplicationReview;
import co.istad.ai_interview_app.features.moderator.repository.CandidateApplicationReviewRepository;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSeekerApplicationServiceImpl implements JobSeekerApplicationService {

    private final AuthenticatedJobSeekerProfileResolver seekerProfileResolver;
    private final JobPostRepository jobPostRepository;
    private final ResumeRepository resumeRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobApplicationMapper applicationMapper;
    private final CandidateApplicationReviewRepository reviewRepository;
    private final AiInterviewSessionRepository aiInterviewSessionRepository;
    private final ApplicationSettingsService applicationSettingsService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public JobApplicationResponse apply(Long jobId, JobApplicationCreateRequest request) {
        JobSeekerProfile seekerProfile = seekerProfileResolver.resolve();
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job post was not found"));
        validateJobAcceptsApplications(jobPost);

        /*
         * Only a live application blocks a new one. A candidate whose earlier
         * attempt was rejected — or who withdrew — may apply again, and the
         * closed attempt stays on record rather than being overwritten, so the
         * history of who was rejected and why survives the second try.
         */
        if (applicationRepository.existsLiveApplication(jobPost.getId(), seekerProfile.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have an open application for this job"
            );
        }

        enforceReapplyCooldown(jobPost.getId(), seekerProfile.getId());

        JobApplication application = new JobApplication();
        application.setJobPost(jobPost);
        application.setJobSeekerProfile(seekerProfile);
        application.setResume(resolveOwnedResume(request.resumeId(), seekerProfile.getId()));
        application.setCoverLetter(normalizeBlankToNull(request.coverLetter()));
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setAppliedAt(Instant.now());

        try {
            JobApplication saved = applicationRepository.saveAndFlush(application);

            /*
             * The moderator queue is built from review rows, not from
             * applications. Creating one here is what puts a new application in
             * front of a moderator at all — before this, a row only appeared
             * once the candidate had finished and scored an AI interview, so
             * anyone who simply applied was invisible.
             *
             * In the same transaction as the application on purpose: an
             * application without its review row is one nobody will ever see.
             */
            createPendingReview(saved);
            adoptPracticeInterviews(saved);

            events.publishEvent(new NotificationEvents.JobApplicationSubmitted(saved.getId()));

            return applicationMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            /*
             * A database constraint refused the row after the checks above
             * passed. Two very different causes, and they used to share the
             * message the live-application check gives — which made a schema
             * problem read as a normal "you already applied", and cost a long
             * time to tell apart.
             *
             * Normally this is a concurrent second submission caught by the
             * partial unique index. But if V19 has not been applied, the old
             * blanket unique constraint is still present and refuses *every*
             * re-application, no matter how long ago the first one closed.
             */
            log.warn(
                    "Application insert refused by a database constraint for job {} — "
                            + "if this is not a concurrent submission, check that "
                            + "uk_job_applications_job_profile has been dropped by V19",
                    jobPost.getId(),
                    ex
            );

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Could not submit this application. If you were rejected before, "
                            + "the platform may not be accepting re-applications yet."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getMyApplications() {
        return applicationRepository.findAllByJobSeekerProfile_UserAccount_KeycloakUserIdOrderByAppliedAtDesc(AuthUtils.extractUserId())
                .stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationResponse getMyApplication(Long applicationId) {
        return applicationMapper.toResponse(resolveMyApplication(applicationId));
    }

    @Override
    @Transactional
    public JobApplicationResponse withdraw(Long applicationId) {
        JobApplication application = resolveMyApplication(applicationId);
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            return applicationMapper.toResponse(application);
        }
        if (application.getStatus() == ApplicationStatus.HIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hired applications cannot be withdrawn");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setClosedAt(Instant.now());

        return applicationMapper.toResponse(application);
    }

    /**
     * Opens the moderator review for a freshly submitted application.
     *
     * <p>Idempotent by lookup rather than by constraint: the AI interview code
     * creates the same row if it finds none, and that path still runs for
     * practice interviews that were never tied to an application.
     */
    /**
     * Holds a rejected candidate back for the administrator-configured period.
     *
     * <p>Reads the setting on every attempt rather than caching it, so raising
     * or clearing the cooldown takes effect immediately instead of at the next
     * restart. Zero disables it, which is the default.
     *
     * <p>A rejection with no {@code closedAt} predates that column and cannot
     * be timed, so it does not hold anyone back — refusing on an unknown date
     * would be an indefinite ban rather than a cooldown.
     */
    private void enforceReapplyCooldown(Long jobPostId, Long jobSeekerProfileId) {
        int cooldownDays = applicationSettingsService.reapplyCooldownDays();

        if (cooldownDays <= 0) return;

        applicationRepository
                .findRejectedApplicationsNewestFirst(jobPostId, jobSeekerProfileId)
                .stream()
                .findFirst()
                .map(JobApplication::getClosedAt)
                .ifPresent(closedAt -> {
                    Instant availableFrom = closedAt.plus(cooldownDays, ChronoUnit.DAYS);

                    if (Instant.now().isBefore(availableFrom)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "You can apply to this job again on %s".formatted(
                                        DateTimeFormatter.ofPattern("d MMM yyyy")
                                                .withZone(ZoneOffset.UTC)
                                                .format(availableFrom)
                                )
                        );
                    }
                });
    }

    private void createPendingReview(JobApplication application) {
        if (reviewRepository.findByApplication_Id(application.getId()).isPresent()) return;

        CandidateApplicationReview review = new CandidateApplicationReview();
        review.setApplication(application);
        review.setReviewStatus(CandidateApplicationReviewStatus.PENDING);
        reviewRepository.save(review);
    }

    /**
     * Attaches AI interviews the seeker already ran against this job.
     *
     * <p>Practising first and applying second is a normal order, and the
     * interview is the same one either way. Without this the earlier session
     * stays unattached, invisible to the moderator queue and unable to satisfy
     * the approval check — so the candidate would have to sit it again.
     */
    private void adoptPracticeInterviews(JobApplication application) {
        List<AiInterviewSession> practiceSessions = aiInterviewSessionRepository
                .findAllByJobPost_IdAndJobSeeker_IdAndApplicationIsNull(
                        application.getJobPost().getId(),
                        application.getJobSeekerProfile().getUserAccount().getId()
                );

        practiceSessions.forEach(session -> session.setApplication(application));
    }

    private JobApplication resolveMyApplication(Long applicationId) {
        return applicationRepository.findByIdAndJobSeekerProfile_UserAccount_KeycloakUserId(applicationId, AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application was not found for authenticated job seeker"
                ));
    }

    private Resume resolveOwnedResume(Long resumeId, Long seekerProfileId) {
        if (resumeId == null) {
            return null;
        }

        return resumeRepository.findByIdAndJobSeekerProfile_Id(resumeId, seekerProfileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume was not found for authenticated job seeker"
                ));
    }

    private void validateJobAcceptsApplications(JobPost jobPost) {
        if (jobPost.getStatus() != JobStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only published jobs accept applications");
        }
        if (jobPost.getExpiredAt() != null && !jobPost.getExpiredAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expired jobs do not accept applications");
        }
    }
}
