package co.istad.ai_interview_app.features.moderator.service;

import co.istad.ai_interview_app.features.company.dto.CompanyDocumentResponse;
import co.istad.ai_interview_app.features.company.dto.CompanyResponse;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.entity.CompanyDocument;
import co.istad.ai_interview_app.features.company.mapper.CompanyMapper;
import co.istad.ai_interview_app.features.company.repository.CompanyDocumentRepository;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.moderator.dto.CompanyVerificationResponse;
import co.istad.ai_interview_app.features.moderator.dto.CompanyIdentityVisibilityRequest;
import co.istad.ai_interview_app.features.moderator.dto.CompanyMaskedProfileRequest;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyListItemResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorJobListItemResponse;
import co.istad.ai_interview_app.features.moderator.entity.JobModerationLog;
import co.istad.ai_interview_app.features.moderator.repository.JobModerationLogRepository;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.features.job.dto.JobPostResponse;
import co.istad.ai_interview_app.features.job.mapper.JobPostMapper;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.features.moderator.entity.CompanyVerification;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.features.moderator.repository.CompanyVerificationRepository;
import co.istad.ai_interview_app.shared.enums.moderation.ModerationDecision;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import org.springframework.context.ApplicationEventPublisher;
import co.istad.ai_interview_app.features.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModeratorCompanyVerificationServiceImpl implements ModeratorCompanyVerificationService {

    private final CompanyRepository companyRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final CompanyVerificationRepository companyVerificationRepository;
    private final JobPostRepository jobPostRepository;
    private final JobModerationLogRepository jobModerationLogRepository;
    private final JobPostMapper jobPostMapper;
    private final AuthenticatedModeratorProfileResolver moderatorProfileResolver;
    private final CompanyMapper companyMapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public Page<ModeratorCompanyListItemResponse> getCompanies(
            VerificationStatus verificationStatus,
            Pageable pageable
    ) {
        Page<Company> companies = verificationStatus == null
                ? companyRepository.findAll(pageable)
                : companyRepository.findAllByVerificationStatus(verificationStatus, pageable);

        // One query for the page's job counts, then a map lookup per row.
        Map<UUID, long[]> counts = jobCountsFor(companies.getContent());

        return companies.map(company -> toListItemResponse(
                company,
                counts.getOrDefault(company.getId(), NO_JOBS)
        ));
    }

    /** total, published — for a company with no jobs at all. */
    private static final long[] NO_JOBS = {0L, 0L};

    private Map<UUID, long[]> jobCountsFor(List<Company> companies) {
        if (companies.isEmpty()) return Map.of();

        List<UUID> ids = companies.stream().map(Company::getId).toList();

        return jobPostRepository.countByCompanyIds(ids, JobStatus.PUBLISHED).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> new long[]{
                                ((Number) row[1]).longValue(),
                                row[2] == null ? 0L : ((Number) row[2]).longValue()
                        }
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public ModeratorCompanyDetailResponse getCompany(UUID companyId) {
        Company company = resolveCompany(companyId);
        CompanyResponse companyResponse = companyMapper.toResponse(company);
        List<CompanyDocumentResponse> documents = companyDocumentRepository
                .findAllByCompany_IdOrderByCreatedAtDesc(company.getId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        List<CompanyVerificationResponse> verificationHistory = companyVerificationRepository
                .findAllByCompany_IdOrderByVerifiedAtDesc(company.getId())
                .stream()
                .map(this::toVerificationResponse)
                .toList();

        return new ModeratorCompanyDetailResponse(companyResponse, documents, verificationHistory);
    }

    /**
     * Masks or unmasks a company for candidates.
     *
     * <p>Takes effect on the next read of every candidate-facing endpoint;
     * nothing is copied onto the job posts, so there is no half-masked state
     * where some listings caught up and others did not.
     */
    @Override
    @Transactional
    public ModeratorCompanyDetailResponse setIdentityVisibility(
            UUID companyId,
            CompanyIdentityVisibilityRequest request
    ) {
        Company company = resolveCompany(companyId);
        company.setIdentityVisibility(request.visibility());
        companyRepository.save(company);

        return getCompany(companyId);
    }

    /**
     * Sets or clears the stand-in logo shown while the company is masked.
     *
     * <p>Deliberately allowed on an unmasked company: an administrator can put
     * a stand-in in place before masking, so masking does not have to be a
     * two-step act with a blank mark visible in between. It is stored either
     * way and only read while masked.
     */
    @Override
    @Transactional
    public ModeratorCompanyDetailResponse setMaskedProfile(
            UUID companyId,
            CompanyMaskedProfileRequest request
    ) {
        Company company = resolveCompany(companyId);
        company.setMaskedLogoUrl(normalizeBlankToNull(request.maskedLogoUrl()));
        companyRepository.save(company);

        return getCompany(companyId);
    }

    @Override
    @Transactional
    public CompanyVerificationResponse approve(UUID companyId, DecisionRequest request) {
        Company company = resolveCompany(companyId);
        CompanyVerification verification = recordDecision(
                company,
                ModerationDecision.APPROVED,
                normalizeBlankToNull(request.decisionNote())
        );

        company.setVerificationStatus(VerificationStatus.APPROVED);
        company.setStatus(ProfileStatus.ACTIVE);

        events.publishEvent(new NotificationEvents.CompanyVerificationDecided(
                company.getId(), ModerationDecision.APPROVED, request.decisionNote()));

        return toVerificationResponse(verification);
    }

    @Override
    @Transactional
    public CompanyVerificationResponse reject(UUID companyId, DecisionRequest request) {
        String note = requireDecisionNote(request);
        Company company = resolveCompany(companyId);
        CompanyVerification verification = recordDecision(company, ModerationDecision.REJECTED, note);

        company.setVerificationStatus(VerificationStatus.REJECTED);
        company.setStatus(ProfileStatus.PENDING);

        events.publishEvent(new NotificationEvents.CompanyVerificationDecided(
                company.getId(), ModerationDecision.REJECTED, note));

        return toVerificationResponse(verification);
    }

    @Override
    @Transactional
    public CompanyVerificationResponse requestRevision(UUID companyId, DecisionRequest request) {
        String note = requireDecisionNote(request);
        Company company = resolveCompany(companyId);
        CompanyVerification verification = recordDecision(company, ModerationDecision.NEEDS_REVISION, note);

        company.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        company.setStatus(ProfileStatus.PENDING);

        events.publishEvent(new NotificationEvents.CompanyVerificationDecided(
                company.getId(), ModerationDecision.NEEDS_REVISION, note));

        return toVerificationResponse(verification);
    }

    /**
     * Withdraws an approved company's standing.
     *
     * <p>Only an approved company can be suspended: the state means "was
     * cleared, and has had that taken away", which is not something a company
     * still waiting on its first decision can be in. A pending or rejected one
     * is already unable to publish, so there would be nothing to withdraw.
     *
     * <p>The effect is not cosmetic — publishing checks for APPROVED, and so
     * does every candidate-facing job query, so the company's existing posts
     * drop out of the public listings on the next read.
     */
    @Override
    @Transactional
    public CompanyVerificationResponse suspend(UUID companyId, DecisionRequest request) {
        String note = requireDecisionNote(request);
        Company company = resolveCompany(companyId);

        if (company.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only an approved company can be suspended"
            );
        }

        CompanyVerification verification = recordDecision(company, ModerationDecision.SUSPENDED, note);

        company.setVerificationStatus(VerificationStatus.SUSPENDED);
        company.setStatus(ProfileStatus.SUSPENDED);

        events.publishEvent(new NotificationEvents.CompanyVerificationDecided(
                company.getId(), ModerationDecision.SUSPENDED, note));

        return toVerificationResponse(verification);
    }

    /**
     * Lifts a suspension, putting the company back where it was.
     *
     * <p>Straight back to approved rather than through the queue again: the
     * verification that cleared it was never withdrawn, only its standing was,
     * and making a moderator re-read documents they already accepted would be
     * make-work. The note is optional here for the same reason — reinstating
     * takes nothing away from the recruiter.
     */
    @Override
    @Transactional
    public CompanyVerificationResponse reinstate(UUID companyId, DecisionRequest request) {
        Company company = resolveCompany(companyId);

        if (company.getVerificationStatus() != VerificationStatus.SUSPENDED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a suspended company can be reinstated"
            );
        }

        String note = normalizeBlankToNull(request.decisionNote());
        CompanyVerification verification = recordDecision(company, ModerationDecision.REINSTATED, note);

        company.setVerificationStatus(VerificationStatus.APPROVED);
        company.setStatus(ProfileStatus.ACTIVE);

        events.publishEvent(new NotificationEvents.CompanyVerificationDecided(
                company.getId(), ModerationDecision.REINSTATED, note));

        return toVerificationResponse(verification);
    }

    /* ------------------------------------------------------------- jobs --- */

    /**
     * One job, whatever state it is in.
     *
     * <p>Not the public endpoint: that one serves only published jobs of
     * approved companies, which is precisely the set a moderator does not need
     * help finding. Drafts, paused and closed posts all resolve here.
     */
    @Override
    @Transactional(readOnly = true)
    public JobPostResponse getJob(UUID jobId) {
        return jobPostMapper.toResponse(resolveJob(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModeratorJobListItemResponse> getCompanyJobs(UUID companyId, Pageable pageable) {
        // Resolved first so a bad id is a 404 rather than an empty page that
        // reads as "this company has posted nothing".
        resolveCompany(companyId);

        return jobPostRepository
                .findAllByCompany_IdOrderByCreatedAtDesc(companyId, pageable)
                .map(this::toJobListItemResponse);
    }

    /**
     * Takes a live posting down.
     *
     * <p>Paused rather than closed: this is a moderator saying "not while we
     * look at this", and the recruiter can be told why and fix it. Closing is
     * the irreversible one and is a separate act.
     */
    @Override
    @Transactional
    public ModeratorJobListItemResponse pauseJob(UUID jobId, DecisionRequest request) {
        String note = requireDecisionNote(request);
        JobPost job = resolveJob(jobId);

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a published job can be paused"
            );
        }

        job.setStatus(JobStatus.PAUSED);
        recordJobDecision(job, ModerationDecision.REJECTED, note);

        return toJobListItemResponse(job);
    }

    @Override
    @Transactional
    public ModeratorJobListItemResponse resumeJob(UUID jobId, DecisionRequest request) {
        JobPost job = resolveJob(jobId);

        if (job.getStatus() != JobStatus.PAUSED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a paused job can be resumed"
            );
        }

        // The same gate the recruiter's own publish goes through: a job cannot
        // come back while the company behind it is suspended.
        Company company = job.getCompany();
        if (company.getVerificationStatus() != VerificationStatus.APPROVED
                || company.getStatus() != ProfileStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The company must be approved and active before its jobs can be resumed"
            );
        }

        job.setStatus(JobStatus.PUBLISHED);
        recordJobDecision(job, ModerationDecision.APPROVED, normalizeBlankToNull(request.decisionNote()));

        return toJobListItemResponse(job);
    }

    @Override
    @Transactional
    public ModeratorJobListItemResponse closeJob(UUID jobId, DecisionRequest request) {
        String note = requireDecisionNote(request);
        JobPost job = resolveJob(jobId);

        if (job.getStatus() == JobStatus.CLOSED) {
            return toJobListItemResponse(job);
        }

        job.setStatus(JobStatus.CLOSED);
        recordJobDecision(job, ModerationDecision.REJECTED, note);

        return toJobListItemResponse(job);
    }

    private JobPost resolveJob(UUID jobId) {
        return jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job was not found"));
    }

    private void recordJobDecision(JobPost job, ModerationDecision action, String note) {
        JobModerationLog log = new JobModerationLog();
        log.setJobPost(job);
        log.setModeratorProfile(moderatorProfileResolver.resolve());
        log.setAction(action);
        log.setNote(note);

        jobModerationLogRepository.save(log);
    }

    private ModeratorJobListItemResponse toJobListItemResponse(JobPost job) {
        return new ModeratorJobListItemResponse(
                job.getId(),
                job.getTitle(),
                job.getLocation(),
                job.getJobType(),
                job.getWorkMode(),
                job.getStatus(),
                job.getPublishedAt(),
                job.getExpiredAt(),
                job.getCreatedAt()
        );
    }

    private Company resolveCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company was not found"));
    }

    private CompanyVerification recordDecision(
            Company company,
            ModerationDecision decision,
            String note
    ) {
        ModeratorProfile moderator = moderatorProfileResolver.resolve();

        CompanyVerification verification = new CompanyVerification();
        verification.setCompany(company);
        verification.setModeratorProfile(moderator);
        verification.setDecision(decision);
        verification.setNote(note);
        verification.setVerifiedAt(Instant.now());

        return companyVerificationRepository.save(verification);
    }

    private String requireDecisionNote(DecisionRequest request) {
        String note = normalizeBlankToNull(request.decisionNote());
        if (!hasText(note)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision note is required");
        }
        return note;
    }

    private ModeratorCompanyListItemResponse toListItemResponse(Company company, long[] jobCounts) {
        return new ModeratorCompanyListItemResponse(
                company.getId(),
                company.getRecruiterProfile().getId(),
                company.getIndustry() == null ? null : company.getIndustry().getId(),
                company.getIndustry() == null ? null : company.getIndustry().getName(),
                company.getName(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                company.getContactEmail(),
                company.getBusinessRegistrationNo(),
                company.getVerificationStatus(),
                company.getStatus(),
                company.getIdentityVisibility(),
                jobCounts[0],
                jobCounts[1]
        );
    }

    private CompanyDocumentResponse toDocumentResponse(CompanyDocument document) {
        return new CompanyDocumentResponse(
                document.getId(),
                document.getCompany().getId(),
                document.getUploadedByRecruiterProfile().getId(),
                document.getDocumentType(),
                document.getDocumentUrl(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }

    private CompanyVerificationResponse toVerificationResponse(CompanyVerification verification) {
        return new CompanyVerificationResponse(
                verification.getId(),
                verification.getCompany().getId(),
                verification.getModeratorProfile().getId(),
                verification.getDecision(),
                verification.getNote(),
                verification.getVerifiedAt()
        );
    }
}
