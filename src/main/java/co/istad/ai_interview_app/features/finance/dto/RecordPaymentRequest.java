package co.istad.ai_interview_app.features.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records money received against an invoice.
 *
 * <p>Entered by finance staff rather than captured from a gateway — nothing on
 * this platform takes payments, so every payment row is a human recording
 * something that happened elsewhere.
 */
public record RecordPaymentRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @Size(max = 80) String paymentMethod,
        @Size(max = 150) String transactionReference,
        Instant paidAt,
        @Size(max = 2000) String note
) {
}
