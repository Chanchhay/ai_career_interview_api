package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.admin.entity.AdminProfile;
import co.istad.ai_interview_app.shared.enums.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {

    Optional<AdminProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);

    List<AdminProfile> findByStatus(ProfileStatus status);
}