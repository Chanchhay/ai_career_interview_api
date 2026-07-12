package co.istad.ai_interview_app.features.recruiter.talent.service;

import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicResumeDownloadResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicTalentDetailResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicTalentListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecruiterTalentService {

    Page<PublicTalentListItemResponse> findPublicTalent(
            String keyword,
            String preferredLocation,
            String availabilityStatus,
            Pageable pageable
    );

    PublicTalentDetailResponse getPublicTalent(String publicProfileSlug);

    PublicResumeDownloadResponse getPublicResumeDownload(String publicProfileSlug, Long resumeId);
}
