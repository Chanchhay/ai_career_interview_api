package co.istad.ai_interview_app.features.finance.dto;

import co.istad.ai_interview_app.shared.enums.finance.HiringRecordStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record HiringRecordResponse(
        Long id,
        Long applicationId,
        Long jobPostId,
        String jobTitle,
        Long companyId,
        String companyName,
        Long jobSeekerProfileId,
        /** The candidate's headline — this platform does not carry real names. */
        String candidateLabel,
        Instant hiredAt,
        BigDecimal offeredSalary,
        String salaryCurrency,
        String note,
        HiringRecordStatus status,
        Instant reviewedAt,
        String reviewNote,
        /** Present once confirmed; null while reported or rejected. */
        CommissionRecordResponse commission
) {
}
