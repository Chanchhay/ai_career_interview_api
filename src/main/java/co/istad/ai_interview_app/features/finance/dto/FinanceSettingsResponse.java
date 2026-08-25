package co.istad.ai_interview_app.features.finance.dto;

import java.math.BigDecimal;

public record FinanceSettingsResponse(
        /** Percentage of the offered salary charged as commission. */
        BigDecimal commissionRate,
        /** Days from issue to due date, used when an invoice names none. */
        int paymentTermsDays,
        String currency
) {
}
