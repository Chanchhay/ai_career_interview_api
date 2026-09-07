package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.features.finance.dto.HireReviewRequest;
import co.istad.ai_interview_app.features.finance.dto.HiringRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.ReportHireRequest;
import co.istad.ai_interview_app.shared.enums.finance.HiringRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface HiringRecordService {

    /* Recruiter. */

    HiringRecordResponse reportHire(UUID applicationId, ReportHireRequest request);

    Page<HiringRecordResponse> findMyCompanyHires(Pageable pageable);

    /* Moderator. */

    Page<HiringRecordResponse> findAll(HiringRecordStatus status, Pageable pageable);

    HiringRecordResponse get(UUID hiringRecordId);

    HiringRecordResponse confirm(UUID hiringRecordId, HireReviewRequest request);

    HiringRecordResponse reject(UUID hiringRecordId, HireReviewRequest request);
}
