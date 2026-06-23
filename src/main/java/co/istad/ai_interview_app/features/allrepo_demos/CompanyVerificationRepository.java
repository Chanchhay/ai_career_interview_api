package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.moderator.entity.CompanyVerification;
import co.istad.ai_interview_app.shared.enums.ModerationDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface CompanyVerificationRepository extends JpaRepository<CompanyVerification, Long> {

    List<CompanyVerification> findByCompany_Id(Long companyId);

    List<CompanyVerification> findByModeratorProfile_Id(Long moderatorProfileId);

    List<CompanyVerification> findByDecision(ModerationDecision decision);
}