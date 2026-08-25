package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.finance.dto.BillableCompanyResponse;
import co.istad.ai_interview_app.features.finance.dto.CommissionRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.CreateInvoiceRequest;
import co.istad.ai_interview_app.features.finance.dto.InvoiceResponse;
import co.istad.ai_interview_app.features.finance.dto.RecordPaymentRequest;
import co.istad.ai_interview_app.features.finance.entity.CommissionRecord;
import co.istad.ai_interview_app.features.finance.entity.FinanceProfile;
import co.istad.ai_interview_app.features.finance.entity.Invoice;
import co.istad.ai_interview_app.features.finance.entity.InvoiceItem;
import co.istad.ai_interview_app.features.finance.entity.InvoicePayment;
import co.istad.ai_interview_app.features.finance.repository.CommissionRecordRepository;
import co.istad.ai_interview_app.features.finance.repository.FinanceProfileRepository;
import co.istad.ai_interview_app.features.finance.repository.InvoiceItemRepository;
import co.istad.ai_interview_app.features.finance.repository.InvoicePaymentRepository;
import co.istad.ai_interview_app.features.finance.repository.InvoiceRepository;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.notification.event.NotificationEvents;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Billing companies for confirmed hires.
 *
 * <p>An invoice is a batch: finance picks which of a company's unbilled
 * commissions go on it. That is why {@code InvoiceItem} points at a commission
 * rather than the commission carrying an invoice id — one bill, many hires, and
 * a disputed hire can simply be left off this month's.
 */
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final CommissionRecordRepository commissionRecordRepository;
    private final CompanyRepository companyRepository;
    private final FinanceProfileRepository financeProfileRepository;
    private final IdentityUserAccountRepository userAccountRepository;
    private final FinanceSettingsService financeSettingsService;
    private final FinanceResponseMapper mapper;
    private final ApplicationEventPublisher events;

    /* ------------------------------------------------------ commissions --- */

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionRecordResponse> findCommissions(
            Long companyId,
            PaymentStatus status,
            Pageable pageable
    ) {
        Page<CommissionRecord> page;

        if (companyId != null) {
            page = commissionRecordRepository.findAllByCompany_IdOrderByCreatedAtDesc(companyId, pageable);
        } else if (status != null) {
            page = commissionRecordRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = commissionRecordRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissionRecordResponse> findUnbilledCommissions(Long companyId) {
        return commissionRecordRepository.findUnbilledByCompany(companyId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillableCompanyResponse> findBillableCompanies() {
        return commissionRecordRepository.findBillableCompanies();
    }

    /* --------------------------------------------------------- invoices --- */

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findInvoices(Long companyId, InvoiceStatus status, Pageable pageable) {
        Page<Invoice> page;

        if (companyId != null) {
            page = invoiceRepository.findAllByCompany_IdOrderByCreatedAtDesc(companyId, pageable);
        } else if (status != null) {
            page = invoiceRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = invoiceRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        // Payments are omitted from list rows on purpose: nobody reads a payment
        // history from an index, and fetching it per row would be N+1 for
        // nothing.
        return page.map(invoice -> mapper.toResponse(
                invoice,
                invoiceItemRepository.findAllByInvoice_IdOrderByIdAsc(invoice.getId()),
                null
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long invoiceId) {
        return detail(resolve(invoiceId));
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company was not found"));

        Set<Long> requestedIds = new LinkedHashSet<>(request.commissionRecordIds());
        List<CommissionRecord> commissions = commissionRecordRepository.findAllByIdIn(requestedIds);

        if (commissions.size() != requestedIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "One or more commission records were not found"
            );
        }

        /*
         * Both checks below matter for the same reason: an invoice that mixed
         * companies, or that billed a commission a second time, would be wrong
         * in a way nobody notices until a customer disputes it.
         */
        for (CommissionRecord commission : commissions) {
            if (!commission.getCompany().getId().equals(company.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Commission %d belongs to a different company".formatted(commission.getId())
                );
            }

            boolean alreadyBilled = invoiceItemRepository
                    .findFirstByCommissionRecord_IdAndInvoice_StatusNot(commission.getId(), InvoiceStatus.CANCELLED)
                    .isPresent();

            if (alreadyBilled) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Commission %d is already on an invoice".formatted(commission.getId())
                );
            }
        }

        FinanceProfile financeProfile = currentFinanceProfile();
        var settings = financeSettingsService.getSettings();

        BigDecimal subtotal = commissions.stream()
                .map(CommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = request.taxAmount() == null ? BigDecimal.ZERO : request.taxAmount();

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setFinanceProfile(financeProfile);
        invoice.setSubtotalAmount(subtotal);
        invoice.setTaxAmount(tax);
        invoice.setTotalAmount(subtotal.add(tax));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setCurrency(commissions.getFirst().getCurrency());
        invoice.setDueAt(request.dueAt() == null
                ? Instant.now().plus(settings.paymentTermsDays(), ChronoUnit.DAYS)
                : request.dueAt());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setNote(normalizeBlankToNull(request.note()));
        invoice.setInvoiceNo(placeholderInvoiceNo());

        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        // Numbered from the id, which the database has just assigned. Deriving
        // it from a count would race two concurrent invoices onto one number.
        saved.setInvoiceNo(invoiceNo(saved.getId()));

        for (CommissionRecord commission : commissions) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(saved);
            item.setCommissionRecord(commission);
            item.setDescription("Placement commission — %s (%s%%)".formatted(
                    commission.getHiringRecord().getJobPost().getTitle(),
                    commission.getCommissionRate().stripTrailingZeros().toPlainString()
            ));
            item.setQuantity(1);
            item.setUnitAmount(commission.getCommissionAmount());
            item.setTotalAmount(commission.getCommissionAmount());
            invoiceItemRepository.save(item);
        }

        return detail(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse issueInvoice(Long invoiceId) {
        Invoice invoice = resolve(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a draft invoice can be issued"
            );
        }

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now());

        events.publishEvent(new NotificationEvents.InvoiceIssued(invoice.getId()));

        return detail(invoice);
    }

    /**
     * Cancels an invoice, returning its commissions to the unbilled pool.
     *
     * <p>A paid invoice cannot be cancelled: money has changed hands, and the
     * correction for that is a credit note, not a deletion.
     */
    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(Long invoiceId) {
        Invoice invoice = resolve(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A paid invoice cannot be cancelled"
            );
        }

        if (invoice.getPaidAmount().signum() > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An invoice with recorded payments cannot be cancelled"
            );
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);

        return detail(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse recordPayment(Long invoiceId, RecordPaymentRequest request) {
        Invoice invoice = resolve(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Issue the invoice before recording payments against it"
            );
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A cancelled invoice cannot take payments"
            );
        }

        InvoicePayment payment = new InvoicePayment();
        payment.setInvoice(invoice);
        payment.setAmount(request.amount());
        payment.setCurrency(invoice.getCurrency());
        payment.setPaymentMethod(normalizeBlankToNull(request.paymentMethod()));
        payment.setTransactionReference(normalizeBlankToNull(request.transactionReference()));
        payment.setPaidAt(request.paidAt() == null ? Instant.now() : request.paidAt());
        payment.setStatus(PaymentStatus.PAID);
        payment.setNote(normalizeBlankToNull(request.note()));
        invoicePaymentRepository.saveAndFlush(payment);

        applyPaymentTotals(invoice);

        return detail(invoice);
    }

    /* --------------------------------------------------------- recruiter --- */

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findMyCompanyInvoices(Pageable pageable) {
        Long companyId = myCompanyId();

        return invoiceRepository
                .findAllByCompany_IdOrderByCreatedAtDesc(companyId, pageable)
                .map(invoice -> mapper.toResponse(
                        invoice,
                        invoiceItemRepository.findAllByInvoice_IdOrderByIdAsc(invoice.getId()),
                        null
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getMyCompanyInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository
                .findByIdAndCompany_Id(invoiceId, myCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice was not found"));

        return detail(invoice);
    }

    /* ----------------------------------------------------------- helpers --- */

    /**
     * Recomputes the invoice total from its payment rows and moves its status.
     *
     * <p>Summed rather than incremented: a running total that drifts from the
     * rows behind it is the classic accounting bug, and recomputing costs one
     * aggregate query.
     */
    private void applyPaymentTotals(Invoice invoice) {
        BigDecimal paid = invoicePaymentRepository
                .sumAmountByInvoiceAndStatus(invoice.getId(), PaymentStatus.PAID)
                .orElse(BigDecimal.ZERO);

        invoice.setPaidAmount(paid);

        boolean settled = paid.compareTo(invoice.getTotalAmount()) >= 0;

        if (settled) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(Instant.now());
            markCommissionsPaid(invoice);
            events.publishEvent(new NotificationEvents.InvoicePaid(invoice.getId()));
        } else if (paid.signum() > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            invoice.setPaidAt(null);
        }
    }

    /**
     * A commission is settled when the invoice carrying it is. Partial payment
     * deliberately settles nothing — there is no principled way to say which of
     * five hires a half payment covered.
     */
    private void markCommissionsPaid(Invoice invoice) {
        Instant paidAt = Instant.now();

        for (InvoiceItem item : invoiceItemRepository.findAllByInvoice_IdOrderByIdAsc(invoice.getId())) {
            CommissionRecord commission = item.getCommissionRecord();
            if (commission == null) continue;

            commission.setStatus(PaymentStatus.PAID);
            commission.setPaidAt(paidAt);
        }
    }

    private Invoice resolve(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice was not found"));
    }

    private InvoiceResponse detail(Invoice invoice) {
        return mapper.toResponse(
                invoice,
                invoiceItemRepository.findAllByInvoice_IdOrderByIdAsc(invoice.getId()),
                invoicePaymentRepository.findAllByInvoice_IdOrderByIdAsc(invoice.getId())
        );
    }

    private Long myCompanyId() {
        return companyRepository
                .findByRecruiterProfile_UserAccount_KeycloakUserId(AuthUtils.extractUserId())
                .map(Company::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No company was found for authenticated recruiter"
                ));
    }

    /**
     * The finance profile an invoice is attributed to, created if the caller
     * has none.
     *
     * <p>Same reasoning as the moderator resolver: SecurityConfig has already
     * decided this caller may run the finance desk, so a missing profile row is
     * a provisioning gap rather than a permission answer. Without this, an
     * administrator — who holds an AdminProfile and reaches these endpoints
     * through the role hierarchy — could read every finance screen and then get
     * a bare 404 on the one action that matters.
     */
    private FinanceProfile currentFinanceProfile() {
        String keycloakUserId = AuthUtils.extractUserId();

        return financeProfileRepository
                .findByUserAccount_KeycloakUserId(keycloakUserId)
                .orElseGet(() -> {
                    UserAccount account = userAccountRepository
                            .findByKeycloakUserId(keycloakUserId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "User account was not found for authenticated user"
                            ));

                    FinanceProfile profile = new FinanceProfile();
                    profile.setUserAccount(account);

                    return financeProfileRepository.save(profile);
                });
    }

    /** Unique and obviously temporary, replaced as soon as the row has an id. */
    private String placeholderInvoiceNo() {
        return "DRAFT-" + java.util.UUID.randomUUID();
    }

    private String invoiceNo(Long invoiceId) {
        int year = Instant.now().atZone(ZoneOffset.UTC).getYear();
        return "INV-%d-%06d".formatted(year, invoiceId);
    }
}
