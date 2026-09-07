package co.istad.ai_interview_app.features.finance.repository;

import co.istad.ai_interview_app.features.finance.entity.HiringRecord;
import co.istad.ai_interview_app.shared.enums.finance.HiringRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HiringRecordRepository extends JpaRepository<HiringRecord, UUID> {

    /** One hire per application — the table's unique constraint, asked as a question. */
    Optional<HiringRecord> findByApplication_Id(UUID applicationId);

    boolean existsByApplication_Id(UUID applicationId);

    Page<HiringRecord> findAllByStatusOrderByCreatedAtDesc(HiringRecordStatus status, Pageable pageable);

    Page<HiringRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<HiringRecord> findAllByCompany_IdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);
}
