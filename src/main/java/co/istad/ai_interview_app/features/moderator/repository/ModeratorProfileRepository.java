package co.istad.ai_interview_app.features.moderator.repository;

import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModeratorProfileRepository extends JpaRepository<ModeratorProfile, Long> {

    Optional<ModeratorProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);
}
