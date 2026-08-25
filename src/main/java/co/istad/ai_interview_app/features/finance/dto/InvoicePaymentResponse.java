package co.istad.ai_interview_app.features.finance.dto;

import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoicePaymentResponse(
        Long id,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String transactionReference,
        Instant paidAt,
        PaymentStatus status,
        String note
) {
}
