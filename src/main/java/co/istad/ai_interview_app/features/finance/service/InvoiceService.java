package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.features.finance.dto.BillableCompanyResponse;
import co.istad.ai_interview_app.features.finance.dto.CommissionRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.CreateInvoiceRequest;
import co.istad.ai_interview_app.features.finance.dto.InvoiceResponse;
import co.istad.ai_interview_app.features.finance.dto.RecordPaymentRequest;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    /* Finance. */

    Page<CommissionRecordResponse> findCommissions(UUID companyId, PaymentStatus status, Pageable pageable);

    List<CommissionRecordResponse> findUnbilledCommissions(UUID companyId);

    /** Every company with something billable, newest debt last. */
    List<BillableCompanyResponse> findBillableCompanies();

    Page<InvoiceResponse> findInvoices(UUID companyId, InvoiceStatus status, Pageable pageable);

    InvoiceResponse getInvoice(UUID invoiceId);

    InvoiceResponse createInvoice(CreateInvoiceRequest request);

    InvoiceResponse issueInvoice(UUID invoiceId);

    InvoiceResponse cancelInvoice(UUID invoiceId);

    InvoiceResponse recordPayment(UUID invoiceId, RecordPaymentRequest request);

    /* Recruiter, read-only and scoped to their own company. */

    Page<InvoiceResponse> findMyCompanyInvoices(Pageable pageable);

    InvoiceResponse getMyCompanyInvoice(UUID invoiceId);
}
