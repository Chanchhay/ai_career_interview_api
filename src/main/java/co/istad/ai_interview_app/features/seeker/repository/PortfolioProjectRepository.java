package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, UUID> {

    Optional<PortfolioProject> findByIdAndPortfolio_Id(UUID id, UUID portfolioId);

    List<PortfolioProject> findAllByPortfolio_IdOrderByDisplayOrderAscCreatedAtDesc(UUID portfolioId);

    void deleteAllByPortfolio_Id(UUID portfolioId);
}
