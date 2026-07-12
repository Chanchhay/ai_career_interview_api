package co.istad.ai_interview_app.features.recruiter.talent.dto;

import java.util.List;

public record PublicTalentDetailResponse(
        PublicTalentListItemResponse profile,
        List<PublicPortfolioResponse> portfolios,
        List<PublicResumeResponse> resumes
) {
}
