package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.finance.entity.CommissionRecord;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface CommissionRecordRepository extends JpaRepository<CommissionRecord, Long> {

    Optional<CommissionRecord> findByHiringRecord_Id(Long hiringRecordId);

    List<CommissionRecord> findByCompany_Id(Long companyId);

    List<CommissionRecord> findByStatus(PaymentStatus status);
}