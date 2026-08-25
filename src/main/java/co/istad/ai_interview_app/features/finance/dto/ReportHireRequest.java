package co.istad.ai_interview_app.features.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A recruiter reporting that they hired a forwarded candidate.
 *
 * <p>The salary is required rather than optional: it is the base the commission
 * is calculated from, and a hire with no salary is a bill nobody can compute.
 */
public record ReportHireRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal offeredSalary,
        @Size(max = 10) String salaryCurrency,
        @Size(max = 2000) String note
) {
}
