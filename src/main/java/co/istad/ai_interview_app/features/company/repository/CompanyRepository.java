package co.istad.ai_interview_app.features.company.repository;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByBusinessRegistrationNo(String businessRegistrationNo);

    boolean existsByBusinessRegistrationNoAndIdNot(String businessRegistrationNo, UUID id);

    boolean existsByRecruiterProfile_Id(UUID recruiterProfileId);

    Optional<Company> findByRecruiterProfile_UserAccount_KeycloakUserId(String keycloakUserId);

    Optional<Company> findFirstByRecruiterProfile_Id(UUID recruiterProfileId);

    /** Batch form, so mapping a list of skills to their authors is one query. */
    List<Company> findAllByRecruiterProfile_IdIn(Collection<UUID> recruiterProfileIds);

    Optional<Company> findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(UUID id, String keycloakUserId);

    Page<Company> findAllByVerificationStatus(VerificationStatus verificationStatus, Pageable pageable);
}
