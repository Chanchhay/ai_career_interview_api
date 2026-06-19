package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job_seeker.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByJobSeekerProfile_Id(Long jobSeekerProfileId);

    Optional<Portfolio> findByPublicUrl(String publicUrl);
}