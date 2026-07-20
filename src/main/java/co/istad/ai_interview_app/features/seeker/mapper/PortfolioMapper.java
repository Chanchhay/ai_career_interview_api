package co.istad.ai_interview_app.features.seeker.mapper;

import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioResponse;
import co.istad.ai_interview_app.features.seeker.entity.Portfolio;
import co.istad.ai_interview_app.features.seeker.entity.PortfolioProject;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioMapper {

    public PortfolioResponse toResponse(Portfolio portfolio, List<PortfolioProject> projects) {
        return new PortfolioResponse(
                portfolio.getId(),
                portfolio.getTitle(),
                portfolio.getSummary(),
                portfolio.getPublicUrl(),
                portfolio.getVisibility(),
                portfolio.getPublishedAt(),
                portfolio.getStatus(),
                projects.stream()
                        .map(this::toProjectResponse)
                        .toList(),
                portfolio.getCreatedAt(),
                portfolio.getUpdatedAt()
        );
    }

    public PortfolioProjectResponse toProjectResponse(PortfolioProject project) {
        return new PortfolioProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getProjectUrl(),
                project.getGithubUrl(),
                project.getImageUrl(),
                project.getTechStack(),
                project.getDisplayOrder(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
