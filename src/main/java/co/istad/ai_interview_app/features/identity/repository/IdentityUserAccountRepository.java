package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.account.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdentityUserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByKeycloakUserId(String keycloakUserId);

    List<UserAccount> findAllByKeycloakUserIdIn(Collection<String> keycloakUserIds);

    /**
     * Just the status column, for the per-request suspension check. A
     * projection rather than the whole entity because that check runs on every
     * authenticated request and has no use for the rest of the row.
     */
    @Query("""
            select userAccount.status
            from UserAccount userAccount
            where userAccount.keycloakUserId = :keycloakUserId
            """)
    Optional<AccountStatus> findStatusByKeycloakUserId(@Param("keycloakUserId") String keycloakUserId);
}
