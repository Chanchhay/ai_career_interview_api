package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.features.finance.dto.HireReviewRequest;
import co.istad.ai_interview_app.features.finance.dto.HiringRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.ReportHireRequest;
import co.istad.ai_interview_app.shared.enums.finance.HiringRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HiringRecordService {

    /* Recruiter. */

    HiringRecordResponse reportHire(Long applicationId, ReportHireRequest request);

    Page<HiringRecordResponse> findMyCompanyHires(Pageable pageable);

    /* Moderator. */

    Page<HiringRecordResponse> findAll(HiringRecordStatus status, Pageable pageable);

    HiringRecordResponse get(Long hiringRecordId);

    HiringRecordResponse confirm(Long hiringRecordId, HireReviewRequest request);

    HiringRecordResponse reject(Long hiringRecordId, HireReviewRequest request);
}
