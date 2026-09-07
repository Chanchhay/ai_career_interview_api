package co.istad.ai_interview_app.features.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        UUID commissionRecordId,
        String description,
        Integer quantity,
        BigDecimal unitAmount,
        BigDecimal totalAmount
) {
}
