package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, Long> {

    Optional<PortfolioProject> findByIdAndPortfolio_Id(Long id, Long portfolioId);

    List<PortfolioProject> findAllByPortfolio_IdOrderByDisplayOrderAscCreatedAtDesc(Long portfolioId);

    void deleteAllByPortfolio_Id(Long portfolioId);
}
