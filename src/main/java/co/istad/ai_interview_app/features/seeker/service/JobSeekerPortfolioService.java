package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.PortfolioCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectUpdateRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface JobSeekerPortfolioService {

    PortfolioResponse createPortfolio(PortfolioCreateRequest request);

    List<PortfolioResponse> getMyPortfolios();

    PortfolioResponse getMyPortfolio(UUID portfolioId);

    PortfolioResponse updatePortfolio(UUID portfolioId, PortfolioUpdateRequest request);

    void deletePortfolio(UUID portfolioId);

    PortfolioProjectResponse createProject(UUID portfolioId, PortfolioProjectRequest request);

    PortfolioProjectResponse updateProject(UUID portfolioId, UUID projectId, PortfolioProjectUpdateRequest request);

    void deleteProject(UUID portfolioId, UUID projectId);
}
