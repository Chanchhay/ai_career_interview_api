package co.istad.ai_interview_app.features.company.repository;

import co.istad.ai_interview_app.features.company.entity.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, UUID> {

    List<CompanyDocument> findAllByCompany_IdOrderByCreatedAtDesc(UUID companyId);

    Optional<CompanyDocument> findByIdAndCompany_IdAndCompany_RecruiterProfile_UserAccount_KeycloakUserId(
            UUID id,
            UUID companyId,
            String keycloakUserId
    );
}
