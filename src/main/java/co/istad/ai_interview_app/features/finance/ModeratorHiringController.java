package co.istad.ai_interview_app.features.finance;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.finance.dto.HireReviewRequest;
import co.istad.ai_interview_app.features.finance.dto.HiringRecordResponse;
import co.istad.ai_interview_app.features.finance.service.HiringRecordService;
import co.istad.ai_interview_app.shared.enums.finance.HiringRecordStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The review step between a recruiter's claim and a bill.
 *
 * <p>Confirming is the only action on this platform that creates a commission,
 * and the only one that sets an application to HIRED.
 */
@RestController
@RequestMapping("/api/v1/moderator/hiring-records")
@RequiredArgsConstructor
public class ModeratorHiringController {

    private final HiringRecordService hiringRecordService;

    @GetMapping
    public ApiResponse<Page<HiringRecordResponse>> findAll(
            @RequestParam(required = false) HiringRecordStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(hiringRecordService.findAll(status, pageable));
    }

    @GetMapping("/{hiringRecordId}")
    public ApiResponse<HiringRecordResponse> get(
            @PathVariable Long hiringRecordId
    ) {
        return ApiResponse.success(hiringRecordService.get(hiringRecordId));
    }

    @PostMapping("/{hiringRecordId}/confirm")
    public ApiResponse<HiringRecordResponse> confirm(
            @PathVariable Long hiringRecordId,
            @Valid @RequestBody(required = false) HireReviewRequest request
    ) {
        return ApiResponse.success(hiringRecordService.confirm(hiringRecordId, request));
    }

    @PostMapping("/{hiringRecordId}/reject")
    public ApiResponse<HiringRecordResponse> reject(
            @PathVariable Long hiringRecordId,
            @Valid @RequestBody(required = false) HireReviewRequest request
    ) {
        return ApiResponse.success(hiringRecordService.reject(hiringRecordId, request));
    }
}
