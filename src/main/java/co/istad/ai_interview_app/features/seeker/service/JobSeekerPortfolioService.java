package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.PortfolioCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectUpdateRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioUpdateRequest;

import java.util.List;

public interface JobSeekerPortfolioService {

    PortfolioResponse createPortfolio(PortfolioCreateRequest request);

    List<PortfolioResponse> getMyPortfolios();

    PortfolioResponse getMyPortfolio(Long portfolioId);

    PortfolioResponse updatePortfolio(Long portfolioId, PortfolioUpdateRequest request);

    void deletePortfolio(Long portfolioId);

    PortfolioProjectResponse createProject(Long portfolioId, PortfolioProjectRequest request);

    PortfolioProjectResponse updateProject(Long portfolioId, Long projectId, PortfolioProjectUpdateRequest request);

    void deleteProject(Long portfolioId, Long projectId);
}
