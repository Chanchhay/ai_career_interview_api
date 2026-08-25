package co.istad.ai_interview_app.features.finance.dto;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long id,
        Long commissionRecordId,
        String description,
        Integer quantity,
        BigDecimal unitAmount,
        BigDecimal totalAmount
) {
}
