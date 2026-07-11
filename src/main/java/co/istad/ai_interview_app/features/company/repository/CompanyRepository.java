package co.istad.ai_interview_app.features.company.repository;

import co.istad.ai_interview_app.features.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByBusinessRegistrationNo(String businessRegistrationNo);

    boolean existsByBusinessRegistrationNoAndIdNot(String businessRegistrationNo, Long id);

    boolean existsByRecruiterProfile_Id(Long recruiterProfileId);

    Optional<Company> findByRecruiterProfile_UserAccount_KeycloakUserId(String keycloakUserId);

    Optional<Company> findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(Long id, String keycloakUserId);
}
