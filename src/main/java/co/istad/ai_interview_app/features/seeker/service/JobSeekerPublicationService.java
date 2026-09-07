package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.PublicationRequest;
import co.istad.ai_interview_app.features.seeker.dto.PublicationResponse;
import java.util.UUID;

public interface JobSeekerPublicationService {

    PublicationResponse updateProfilePublication(PublicationRequest request);

    PublicationResponse updatePortfolioPublication(UUID portfolioId, PublicationRequest request);

    PublicationResponse updateResumePublication(UUID resumeId, PublicationRequest request);
}
