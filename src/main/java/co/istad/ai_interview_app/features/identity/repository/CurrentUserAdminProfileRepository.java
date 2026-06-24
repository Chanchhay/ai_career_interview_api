package co.istad.ai_interview_app.features.identity.repository;

import co.istad.ai_interview_app.features.admin.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrentUserAdminProfileRepository extends JpaRepository<AdminProfile, Long> {

    Optional<AdminProfile> findByUserAccount_Id(Long userAccountId);
}
