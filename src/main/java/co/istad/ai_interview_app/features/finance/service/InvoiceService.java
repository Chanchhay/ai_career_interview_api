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

public interface InvoiceService {

    /* Finance. */

    Page<CommissionRecordResponse> findCommissions(Long companyId, PaymentStatus status, Pageable pageable);

    List<CommissionRecordResponse> findUnbilledCommissions(Long companyId);

    /** Every company with something billable, newest debt last. */
    List<BillableCompanyResponse> findBillableCompanies();

    Page<InvoiceResponse> findInvoices(Long companyId, InvoiceStatus status, Pageable pageable);

    InvoiceResponse getInvoice(Long invoiceId);

    InvoiceResponse createInvoice(CreateInvoiceRequest request);

    InvoiceResponse issueInvoice(Long invoiceId);

    InvoiceResponse cancelInvoice(Long invoiceId);

    InvoiceResponse recordPayment(Long invoiceId, RecordPaymentRequest request);

    /* Recruiter, read-only and scoped to their own company. */

    Page<InvoiceResponse> findMyCompanyInvoices(Pageable pageable);

    InvoiceResponse getMyCompanyInvoice(Long invoiceId);
}
