package co.istad.ai_interview_app.features.moderator.repository;

import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModeratorProfileRepository extends JpaRepository<ModeratorProfile, Long> {

    Optional<ModeratorProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);

    /**
     * Every active moderator's user-account id, for notifications that address
     * the review queue rather than one person. Ids only — the caller needs
     * nothing else, and loading whole profiles to read one field each would be
     * waste.
     */
    @Query("""
            select moderatorProfile.userAccount.id
            from ModeratorProfile moderatorProfile
            where moderatorProfile.status = :status
            """)
    List<Long> findUserAccountIdsByStatus(@Param("status") ProfileStatus status);
}
