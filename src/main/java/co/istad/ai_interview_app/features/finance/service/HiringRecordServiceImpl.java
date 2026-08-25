package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.finance.dto.CommissionRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.HireReviewRequest;
import co.istad.ai_interview_app.features.finance.dto.HiringRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.ReportHireRequest;
import co.istad.ai_interview_app.features.finance.entity.CommissionRecord;
import co.istad.ai_interview_app.features.finance.entity.HiringRecord;
import co.istad.ai_interview_app.features.finance.repository.CommissionRecordRepository;
import co.istad.ai_interview_app.features.finance.repository.HiringRecordRepository;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.moderator.entity.CandidateApplicationReview;
import co.istad.ai_interview_app.features.moderator.repository.CandidateApplicationReviewRepository;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.finance.HiringRecordStatus;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Recording hires, and turning a confirmed one into a commission.
 *
 * <p>Two steps on purpose. The recruiter is the only party who knows a hire
 * happened, and also the party the commission is charged to; letting the report
 * bill them directly would put the platform's revenue entirely in the hands of
 * whoever owes it. A moderator confirms, and confirmation is what creates the
 * money.
 */
@Service
@RequiredArgsConstructor
public class HiringRecordServiceImpl implements HiringRecordService {

    private final HiringRecordRepository hiringRecordRepository;
    private final CommissionRecordRepository commissionRecordRepository;
    private final CandidateApplicationReviewRepository reviewRepository;
    private final CompanyRepository companyRepository;
    private final IdentityUserAccountRepository userAccountRepository;
    private final FinanceSettingsService financeSettingsService;
    private final FinanceResponseMapper mapper;

    /* --------------------------------------------------------- recruiter --- */

    /**
     * @param applicationId must be an application forwarded to this recruiter —
     *                      the same visibility rule the forwarded-applications
     *                      endpoint enforces, so a recruiter cannot report a
     *                      hire for a candidate they were never shown.
     */
    @Override
    @Transactional
    public HiringRecordResponse reportHire(Long applicationId, ReportHireRequest request) {
        CandidateApplicationReview review = reviewRepository
                .findByApplication_IdAndReviewStatusAndApplication_JobPost_RecruiterProfile_UserAccount_KeycloakUserId(
                        applicationId,
                        CandidateApplicationReviewStatus.FORWARDED,
                        AuthUtils.extractUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Forwarded application was not found for authenticated recruiter"
                ));

        JobApplication application = review.getApplication();

        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A withdrawn application cannot be reported as a hire"
            );
        }

        // Reporting twice returns the existing record rather than failing: the
        // unique constraint would reject it anyway, and a 409 on a button the
        // recruiter may reasonably press again is noise.
        Optional<HiringRecord> existing = hiringRecordRepository.findByApplication_Id(applicationId);
        if (existing.isPresent()) {
            return mapper.toResponse(existing.get(), commissionFor(existing.get()));
        }

        HiringRecord record = new HiringRecord();
        record.setApplication(application);
        record.setJobPost(application.getJobPost());
        record.setCompany(application.getJobPost().getCompany());
        record.setJobSeekerProfile(application.getJobSeekerProfile());
        record.setHiredAt(Instant.now());
        record.setOfferedSalary(request.offeredSalary());
        record.setSalaryCurrency(resolveCurrency(request.salaryCurrency()));
        record.setNote(normalizeBlankToNull(request.note()));
        record.setStatus(HiringRecordStatus.REPORTED);
        record.setReportedByUserAccount(currentUserAccount());

        return mapper.toResponse(hiringRecordRepository.save(record), null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HiringRecordResponse> findMyCompanyHires(Pageable pageable) {
        Long companyId = companyRepository
                .findByRecruiterProfile_UserAccount_KeycloakUserId(AuthUtils.extractUserId())
                .map(company -> company.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No company was found for authenticated recruiter"
                ));

        return hiringRecordRepository
                .findAllByCompany_IdOrderByCreatedAtDesc(companyId, pageable)
                .map(record -> mapper.toResponse(record, commissionFor(record)));
    }

    /* --------------------------------------------------------- moderator --- */

    @Override
    @Transactional(readOnly = true)
    public Page<HiringRecordResponse> findAll(HiringRecordStatus status, Pageable pageable) {
        Page<HiringRecord> page = status == null
                ? hiringRecordRepository.findAllByOrderByCreatedAtDesc(pageable)
                : hiringRecordRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);

        return page.map(record -> mapper.toResponse(record, commissionFor(record)));
    }

    @Override
    @Transactional(readOnly = true)
    public HiringRecordResponse get(Long hiringRecordId) {
        HiringRecord record = resolve(hiringRecordId);
        return mapper.toResponse(record, commissionFor(record));
    }

    /**
     * Confirms a reported hire and creates its commission.
     *
     * <p>The rate is copied onto the commission rather than referenced, so a
     * later change to the platform rate cannot restate a bill that has already
     * been calculated — or worse, one already sent.
     */
    @Override
    @Transactional
    public HiringRecordResponse confirm(Long hiringRecordId, HireReviewRequest request) {
        HiringRecord record = resolve(hiringRecordId);

        if (record.getStatus() == HiringRecordStatus.CONFIRMED) {
            return mapper.toResponse(record, commissionFor(record));
        }

        if (record.getStatus() == HiringRecordStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A rejected hire report cannot be confirmed"
            );
        }

        if (record.getOfferedSalary() == null || record.getOfferedSalary().signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This hire has no offered salary, so no commission can be calculated"
            );
        }

        var settings = financeSettingsService.getSettings();
        BigDecimal rate = settings.commissionRate();
        BigDecimal amount = record.getOfferedSalary()
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        CommissionRecord commission = new CommissionRecord();
        commission.setHiringRecord(record);
        commission.setCompany(record.getCompany());
        commission.setCommissionRate(rate);
        commission.setCommissionAmount(amount);
        commission.setCurrency(resolveCurrency(record.getSalaryCurrency()));
        commission.setDueAt(Instant.now().plus(settings.paymentTermsDays(), ChronoUnit.DAYS));
        commission.setStatus(PaymentStatus.PENDING);
        commissionRecordRepository.save(commission);

        record.setStatus(HiringRecordStatus.CONFIRMED);
        record.setReviewedByUserAccount(currentUserAccount());
        record.setReviewedAt(Instant.now());
        record.setReviewNote(normalizeBlankToNull(request == null ? null : request.note()));

        // The one place the platform records a hire as final. Nothing else in
        // the codebase sets HIRED — every other reference reads it.
        record.getApplication().setStatus(ApplicationStatus.HIRED);

        return mapper.toResponse(record, commission);
    }

    @Override
    @Transactional
    public HiringRecordResponse reject(Long hiringRecordId, HireReviewRequest request) {
        HiringRecord record = resolve(hiringRecordId);

        if (record.getStatus() == HiringRecordStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A confirmed hire cannot be rejected; cancel its commission instead"
            );
        }

        record.setStatus(HiringRecordStatus.REJECTED);
        record.setReviewedByUserAccount(currentUserAccount());
        record.setReviewedAt(Instant.now());
        record.setReviewNote(normalizeBlankToNull(request == null ? null : request.note()));

        return mapper.toResponse(record, null);
    }

    /* ----------------------------------------------------------- helpers --- */

    private HiringRecord resolve(Long hiringRecordId) {
        return hiringRecordRepository.findById(hiringRecordId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hiring record was not found"
                ));
    }

    private CommissionRecord commissionFor(HiringRecord record) {
        if (record.getStatus() != HiringRecordStatus.CONFIRMED) return null;

        return commissionRecordRepository.findByHiringRecord_Id(record.getId()).orElse(null);
    }

    private UserAccount currentUserAccount() {
        return userAccountRepository.findByKeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account was not found for authenticated user"
                ));
    }

    private String resolveCurrency(String requested) {
        String currency = normalizeBlankToNull(requested);
        return currency == null ? financeSettingsService.getSettings().currency() : currency.toUpperCase();
    }
}
