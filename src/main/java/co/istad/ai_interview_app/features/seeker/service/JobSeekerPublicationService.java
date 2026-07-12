package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.PublicationRequest;
import co.istad.ai_interview_app.features.seeker.dto.PublicationResponse;

public interface JobSeekerPublicationService {

    PublicationResponse updateProfilePublication(PublicationRequest request);

    PublicationResponse updatePortfolioPublication(Long portfolioId, PublicationRequest request);

    PublicationResponse updateResumePublication(Long resumeId, PublicationRequest request);
}
