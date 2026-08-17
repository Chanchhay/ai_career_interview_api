package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.Portfolio;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByIdAndJobSeekerProfile_Id(Long id, Long jobSeekerProfileId);

    List<Portfolio> findAllByJobSeekerProfile_IdOrderByCreatedAtDesc(Long jobSeekerProfileId);

    List<Portfolio> findAllByJobSeekerProfile_IdAndStatusAndVisibilityOrderByCreatedAtDesc(
            Long jobSeekerProfileId,
            ProfileStatus status,
            VisibilityStatus visibility
    );
}
