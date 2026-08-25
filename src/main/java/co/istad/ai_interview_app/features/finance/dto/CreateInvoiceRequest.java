package co.istad.ai_interview_app.features.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Bills a company for a chosen set of its unbilled commissions.
 *
 * <p>Commissions are named explicitly rather than swept up automatically:
 * deciding what goes on this month's invoice is a finance judgment, and a
 * disputed hire should be leavable off it.
 */
public record CreateInvoiceRequest(
        @NotNull Long companyId,
        @NotEmpty List<Long> commissionRecordIds,
        @DecimalMin("0.0") BigDecimal taxAmount,
        Instant dueAt,
        @Size(max = 2000) String note
) {
}
