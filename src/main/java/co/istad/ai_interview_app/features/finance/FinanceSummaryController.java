package co.istad.ai_interview_app.features.finance;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.finance.dto.FinanceSummaryResponse;
import co.istad.ai_interview_app.features.finance.dto.FinanceSummaryResponse.PaidCompany;
import co.istad.ai_interview_app.features.finance.service.FinanceReportPeriod;
import co.istad.ai_interview_app.features.finance.service.FinanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Inherits the existing FINANCE / SUPER_ADMIN security rule. */
@RestController
@RequestMapping("/api/v1/finance/summary")
@RequiredArgsConstructor
public class FinanceSummaryController {
    private final FinanceSummaryService service;

    @GetMapping
    public ApiResponse<FinanceSummaryResponse> summary(
            @RequestParam(defaultValue = "MONTH") FinanceReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String currency) {
        return ApiResponse.success(service.summary(period, date, currency));
    }

    @GetMapping("/companies")
    public ApiResponse<Page<PaidCompany>> companies(
            @RequestParam(defaultValue = "MONTH") FinanceReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(service.companies(period, date, currency, search, pageable));
    }
}
