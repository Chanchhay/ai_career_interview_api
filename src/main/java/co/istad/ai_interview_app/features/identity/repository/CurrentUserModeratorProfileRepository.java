package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrentUserModeratorProfileRepository extends JpaRepository<ModeratorProfile, Long> {

    Optional<ModeratorProfile> findByUserAccount_Id(Long userAccountId);
}
