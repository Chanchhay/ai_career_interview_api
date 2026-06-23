package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.shared.enums.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface ModeratorProfileRepository extends JpaRepository<ModeratorProfile, Long> {

    Optional<ModeratorProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);

    List<ModeratorProfile> findByStatus(ProfileStatus status);

    List<ModeratorProfile> findBySpecialization(String specialization);
}