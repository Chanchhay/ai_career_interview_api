package co.istad.ai_interview_app.features.recruiter.talent.controller;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicResumeDownloadResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicTalentDetailResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicTalentListItemResponse;
import co.istad.ai_interview_app.features.recruiter.talent.service.RecruiterTalentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/recruiter/talent")
@RequiredArgsConstructor
public class RecruiterTalentController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id",
            "createdAt",
            "updatedAt",
            "publishedAt",
            "headline",
            "currentPosition",
            "preferredLocation",
            "availabilityStatus"
    );

    private final RecruiterTalentService recruiterTalentService;

    @GetMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<Page<PublicTalentListItemResponse>> findPublicTalent(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String preferredLocation,
            @RequestParam(required = false) String availabilityStatus,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        validateTalentPageable(pageable);

        return ApiResponse.success(recruiterTalentService.findPublicTalent(
                keyword,
                preferredLocation,
                availabilityStatus,
                pageable
        ));
    }

    @GetMapping("/{publicProfileSlug}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<PublicTalentDetailResponse> getPublicTalent(
            @PathVariable String publicProfileSlug
    ) {
        return ApiResponse.success(recruiterTalentService.getPublicTalent(publicProfileSlug));
    }

    @GetMapping("/{publicProfileSlug}/resumes/{resumeId}/download")
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<PublicResumeDownloadResponse> getPublicResumeDownload(
            @PathVariable String publicProfileSlug,
            @PathVariable Long resumeId
    ) {
        return ApiResponse.success(recruiterTalentService.getPublicResumeDownload(publicProfileSlug, resumeId));
    }

    private void validateTalentPageable(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be less than or equal to " + MAX_PAGE_SIZE
            );
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported sort property: " + order.getProperty()
                );
            }
        }
    }
}
