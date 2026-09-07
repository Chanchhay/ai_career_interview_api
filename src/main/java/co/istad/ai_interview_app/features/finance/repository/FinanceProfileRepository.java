package co.istad.ai_interview_app.features.finance.repository;

import co.istad.ai_interview_app.features.finance.entity.FinanceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinanceProfileRepository extends JpaRepository<FinanceProfile, UUID> {

    Optional<FinanceProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);
}
