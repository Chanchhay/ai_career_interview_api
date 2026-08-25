package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.admin.entity.AdminProfile;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentUserAdminProfileRepository extends JpaRepository<AdminProfile, Long> {

    /**
     * Active administrators' user-account ids. Administrators reach every
     * moderator screen through the role hierarchy, so anything addressed to
     * "the moderator team" has to include them or it reaches nobody on a
     * deployment where staff are all administrators.
     */
    @Query("""
            select adminProfile.userAccount.id
            from AdminProfile adminProfile
            where adminProfile.status = :status
            """)
    List<Long> findUserAccountIdsByStatus(@Param("status") ProfileStatus status);

    Optional<AdminProfile> findByUserAccount_Id(Long userAccountId);
}
