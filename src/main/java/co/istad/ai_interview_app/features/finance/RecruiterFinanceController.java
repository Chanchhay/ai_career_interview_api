package co.istad.ai_interview_app.features.finance;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.finance.dto.HiringRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.InvoiceResponse;
import co.istad.ai_interview_app.features.finance.dto.ReportHireRequest;
import co.istad.ai_interview_app.features.finance.service.HiringRecordService;
import co.istad.ai_interview_app.features.finance.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * What a recruiter can do with the money side: report a hire, and read.
 *
 * <p>Reporting is the only write. Recruiters cannot confirm their own hire,
 * issue their own invoice, or record their own payment — each of those is
 * somebody else's job precisely because the recruiter is the one being billed.
 */
@RestController
@RequestMapping("/api/v1/recruiter")
@RequiredArgsConstructor
public class RecruiterFinanceController {

    private final HiringRecordService hiringRecordService;
    private final InvoiceService invoiceService;

    @PostMapping("/forwarded-applications/{applicationId}/hire")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HiringRecordResponse> reportHire(
            @PathVariable Long applicationId,
            @Valid @RequestBody ReportHireRequest request
    ) {
        return ApiResponse.success(hiringRecordService.reportHire(applicationId, request));
    }

    @GetMapping("/hiring-records")
    public ApiResponse<Page<HiringRecordResponse>> findMyHires(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(hiringRecordService.findMyCompanyHires(pageable));
    }

    @GetMapping("/invoices")
    public ApiResponse<Page<InvoiceResponse>> findMyInvoices(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(invoiceService.findMyCompanyInvoices(pageable));
    }

    @GetMapping("/invoices/{invoiceId}")
    public ApiResponse<InvoiceResponse> getMyInvoice(
            @PathVariable Long invoiceId
    ) {
        return ApiResponse.success(invoiceService.getMyCompanyInvoice(invoiceId));
    }
}
