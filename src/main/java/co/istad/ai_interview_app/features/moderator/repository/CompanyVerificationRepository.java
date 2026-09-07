package co.istad.ai_interview_app.features.moderator.repository;

import co.istad.ai_interview_app.features.moderator.entity.CompanyVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompanyVerificationRepository extends JpaRepository<CompanyVerification, UUID> {

    List<CompanyVerification> findAllByCompany_IdOrderByVerifiedAtDesc(UUID companyId);
}
