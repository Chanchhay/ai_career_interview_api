package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.company.entity.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, Long> {

    List<CompanyDocument> findByCompany_Id(Long companyId);

    List<CompanyDocument> findByUploadedByRecruiterProfile_Id(Long recruiterProfileId);
}