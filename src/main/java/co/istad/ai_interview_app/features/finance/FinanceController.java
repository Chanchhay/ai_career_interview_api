package co.istad.ai_interview_app.features.finance;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.finance.dto.CommissionRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.CreateInvoiceRequest;
import co.istad.ai_interview_app.features.finance.dto.FinanceSettingsRequest;
import co.istad.ai_interview_app.features.finance.dto.FinanceSettingsResponse;
import co.istad.ai_interview_app.features.finance.dto.InvoiceResponse;
import co.istad.ai_interview_app.features.finance.dto.RecordPaymentRequest;
import co.istad.ai_interview_app.features.finance.service.FinanceSettingsService;
import co.istad.ai_interview_app.features.finance.service.InvoiceService;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The finance desk — FINANCE role, and SUPER_ADMIN through the role hierarchy.
 *
 * <p>This is the first controller behind the {@code /api/v1/finance/**} rule,
 * which SecurityConfig has carried since before there were any endpoints for it.
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final InvoiceService invoiceService;
    private final FinanceSettingsService financeSettingsService;

    /* ------------------------------------------------------ commissions --- */

    @GetMapping("/commissions")
    public ApiResponse<Page<CommissionRecordResponse>> findCommissions(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(invoiceService.findCommissions(companyId, status, pageable));
    }

    /** What could go on this company's next invoice. */
    @GetMapping("/companies/{companyId}/unbilled-commissions")
    public ApiResponse<List<CommissionRecordResponse>> findUnbilled(
            @PathVariable Long companyId
    ) {
        return ApiResponse.success(invoiceService.findUnbilledCommissions(companyId));
    }

    /* --------------------------------------------------------- invoices --- */

    @GetMapping("/invoices")
    public ApiResponse<Page<InvoiceResponse>> findInvoices(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(invoiceService.findInvoices(companyId, status, pageable));
    }

    @GetMapping("/invoices/{invoiceId}")
    public ApiResponse<InvoiceResponse> getInvoice(
            @PathVariable Long invoiceId
    ) {
        return ApiResponse.success(invoiceService.getInvoice(invoiceId));
    }

    /** Creates a draft. Nothing is sent, and nothing is owed, until it is issued. */
    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request
    ) {
        return ApiResponse.success(invoiceService.createInvoice(request));
    }

    @PostMapping("/invoices/{invoiceId}/issue")
    public ApiResponse<InvoiceResponse> issueInvoice(
            @PathVariable Long invoiceId
    ) {
        return ApiResponse.success(invoiceService.issueInvoice(invoiceId));
    }

    @PostMapping("/invoices/{invoiceId}/cancel")
    public ApiResponse<InvoiceResponse> cancelInvoice(
            @PathVariable Long invoiceId
    ) {
        return ApiResponse.success(invoiceService.cancelInvoice(invoiceId));
    }

    @PostMapping("/invoices/{invoiceId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> recordPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody RecordPaymentRequest request
    ) {
        return ApiResponse.success(invoiceService.recordPayment(invoiceId, request));
    }

    /* --------------------------------------------------------- settings --- */

    @GetMapping("/settings")
    public ApiResponse<FinanceSettingsResponse> getSettings() {
        return ApiResponse.success(financeSettingsService.getSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<FinanceSettingsResponse> updateSettings(
            @Valid @RequestBody FinanceSettingsRequest request
    ) {
        return ApiResponse.success(financeSettingsService.updateSettings(request));
    }
}
