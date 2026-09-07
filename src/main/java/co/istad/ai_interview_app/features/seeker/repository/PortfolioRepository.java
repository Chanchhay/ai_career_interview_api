package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.Portfolio;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    Optional<Portfolio> findByIdAndJobSeekerProfile_Id(UUID id, UUID jobSeekerProfileId);

    List<Portfolio> findAllByJobSeekerProfile_IdOrderByCreatedAtDesc(UUID jobSeekerProfileId);

    List<Portfolio> findAllByJobSeekerProfile_IdAndStatusAndVisibilityOrderByCreatedAtDesc(
            UUID jobSeekerProfileId,
            ProfileStatus status,
            VisibilityStatus visibility
    );
}
