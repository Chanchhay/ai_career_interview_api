package co.istad.ai_interview_app.features.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinanceSummaryResponse(
        String period, LocalDate startDate, LocalDate endDateExclusive,
        LocalDate previousStartDate, String timeZone, String currency,
        List<String> availableCurrencies, Instant generatedAt,
        BigDecimal receivedAmount, BigDecimal previousReceivedAmount,
        long paymentCount, long payingCompanyCount, long invoiceCount,
        BigDecimal outstandingAmount, long outstandingInvoiceCount,
        BigDecimal overdueAmount, long overdueInvoiceCount,
        List<Trend> trend
) {
    public record Trend(LocalDate date, BigDecimal amount, long paymentCount) {}
    public record PaidCompany(UUID companyId, String companyName, String currency, BigDecimal receivedAmount,
                              long paymentCount, long invoiceCount, Instant lastPaymentAt) {}
}
