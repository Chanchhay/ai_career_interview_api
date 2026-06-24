package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityUserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByKeycloakUserId(String keycloakUserId);
}
