package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.finance.domain.FinanceProfile;
import co.istad.ai_interview_app.shared.enums.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface FinanceProfileRepository extends JpaRepository<FinanceProfile, Long> {

    Optional<FinanceProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);

    List<FinanceProfile> findByStatus(ProfileStatus status);
}