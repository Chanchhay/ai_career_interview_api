package co.istad.ai_interview_app.features.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One company with commissions nobody has billed yet.
 *
 * <p>The finance desk's starting point: it answers "who owes us something we
 * have not sent a bill for", which is the question that was previously only
 * answerable by opening companies one at a time.
 *
 * <p>Grouped by currency as well as company, so a company that somehow earned
 * commissions in two currencies appears once per currency — an invoice carries
 * a single currency, so those are genuinely two different bills.
 */
public record BillableCompanyResponse(
        Long companyId,
        String companyName,
        long commissionCount,
        BigDecimal totalAmount,
        String currency,
        /** The earliest due date in the pool — how late this bill already is. */
        Instant oldestDueAt
) {
}
