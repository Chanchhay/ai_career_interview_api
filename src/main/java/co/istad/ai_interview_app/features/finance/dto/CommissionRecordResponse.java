package co.istad.ai_interview_app.features.finance.dto;

import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionRecordResponse(
        Long id,
        Long hiringRecordId,
        Long companyId,
        String companyName,
        /** Frozen at confirmation, so later rate changes never rewrite this. */
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        String currency,
        Instant dueAt,
        Instant paidAt,
        PaymentStatus status,
        String note,
        /** Null until an invoice picks this commission up. */
        Long invoiceId,
        String invoiceNo
) {
}
