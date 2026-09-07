package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrentUserModeratorProfileRepository extends JpaRepository<ModeratorProfile, UUID> {

    Optional<ModeratorProfile> findByUserAccount_Id(UUID userAccountId);
}
