package co.istad.ai_interview_app.features.moderator.service;

import co.istad.ai_interview_app.features.moderator.dto.CompanyIdentityVisibilityRequest;
import co.istad.ai_interview_app.features.moderator.dto.CompanyMaskedProfileRequest;
import co.istad.ai_interview_app.features.moderator.dto.DecisionRequest;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyDetailResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorCompanyListItemResponse;
import co.istad.ai_interview_app.features.moderator.dto.ModeratorJobListItemResponse;
import co.istad.ai_interview_app.features.job.dto.JobPostResponse;
import co.istad.ai_interview_app.features.moderator.dto.CompanyVerificationResponse;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface ModeratorCompanyVerificationService {

    Page<ModeratorCompanyListItemResponse> getCompanies(
            VerificationStatus verificationStatus,
            Pageable pageable
    );

    ModeratorCompanyDetailResponse getCompany(UUID companyId);

    /** Masks or unmasks the company for candidates. */
    ModeratorCompanyDetailResponse setIdentityVisibility(
            UUID companyId,
            CompanyIdentityVisibilityRequest request
    );

    /** Sets or clears the stand-in logo shown while the company is masked. */
    ModeratorCompanyDetailResponse setMaskedProfile(
            UUID companyId,
            CompanyMaskedProfileRequest request
    );

    CompanyVerificationResponse approve(UUID companyId, DecisionRequest request);

    CompanyVerificationResponse reject(UUID companyId, DecisionRequest request);

    CompanyVerificationResponse requestRevision(UUID companyId, DecisionRequest request);

    /**
     * Withdraws an approved company's standing: its jobs stop being published
     * and stop appearing to candidates, without the record being deleted.
     */
    CompanyVerificationResponse suspend(UUID companyId, DecisionRequest request);

    /** Lifts a suspension, returning the company to approved. */
    CompanyVerificationResponse reinstate(UUID companyId, DecisionRequest request);

    /** One job in full, in any state — the console's job detail. */
    JobPostResponse getJob(UUID jobId);

    /** Every job this company has posted, whatever state it is in. */
    Page<ModeratorJobListItemResponse> getCompanyJobs(UUID companyId, Pageable pageable);

    /**
     * Takes a published job out of the candidate-facing listings, leaves it
     * paused, and records who did it and why.
     */
    ModeratorJobListItemResponse pauseJob(UUID jobId, DecisionRequest request);

    /** Puts a paused job back in front of candidates. */
    ModeratorJobListItemResponse resumeJob(UUID jobId, DecisionRequest request);

    /** Closes a job for good; only the recruiter can post a replacement. */
    ModeratorJobListItemResponse closeJob(UUID jobId, DecisionRequest request);
}
