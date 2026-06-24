package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.finance.entity.FinanceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrentUserFinanceProfileRepository extends JpaRepository<FinanceProfile, Long> {

    Optional<FinanceProfile> findByUserAccount_Id(Long userAccountId);
}
