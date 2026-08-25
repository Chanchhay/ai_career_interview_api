package co.istad.ai_interview_app.features.finance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FinanceSettingsRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal commissionRate,
        @Min(0) @Max(365) int paymentTermsDays,
        @Size(min = 3, max = 10) String currency
) {
}
