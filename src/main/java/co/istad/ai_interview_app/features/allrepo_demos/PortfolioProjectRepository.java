package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job_seeker.domain.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, Long> {

    List<PortfolioProject> findByPortfolio_IdOrderByDisplayOrderAsc(Long portfolioId);
}
