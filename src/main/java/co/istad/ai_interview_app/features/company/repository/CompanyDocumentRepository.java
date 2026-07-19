package co.istad.ai_interview_app.features.company.repository;

import co.istad.ai_interview_app.features.company.entity.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, Long> {

    List<CompanyDocument> findAllByCompany_IdOrderByCreatedAtDesc(Long companyId);

    Optional<CompanyDocument> findByIdAndCompany_IdAndCompany_RecruiterProfile_UserAccount_KeycloakUserId(
            Long id,
            Long companyId,
            String keycloakUserId
    );
}
