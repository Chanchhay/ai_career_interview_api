package co.istad.ai_interview_app.features.finance.dto;

import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNo,
        UUID companyId,
        String companyName,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        /** totalAmount minus paidAmount, never below zero. */
        BigDecimal outstandingAmount,
        String currency,
        Instant issuedAt,
        Instant dueAt,
        Instant paidAt,
        InvoiceStatus status,
        String note,
        List<InvoiceItemResponse> items,
        /** Omitted on list responses; present when reading one invoice. */
        List<InvoicePaymentResponse> payments
) {
}
