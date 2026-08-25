package co.istad.ai_interview_app.features.finance.repository;

import co.istad.ai_interview_app.features.finance.entity.CommissionRecord;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

@Repository
public interface CommissionRecordRepository extends JpaRepository<CommissionRecord, Long> {

    /** The commission a confirmed hire produced. One-to-one, enforced by the table. */
    Optional<CommissionRecord> findByHiringRecord_Id(Long hiringRecordId);

    Page<CommissionRecord> findAllByCompany_IdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    Page<CommissionRecord> findAllByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    Page<CommissionRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * A company's commissions that no invoice has picked up yet — the pool
     * finance draws from when billing.
     *
     * <p>"Unbilled" is defined by the absence of an invoice item pointing at the
     * commission rather than by a flag on the commission, so the two can never
     * disagree about what has been invoiced.
     */
    @Query("""
            select commission
            from CommissionRecord commission
            where commission.company.id = :companyId
              and commission.status = co.istad.ai_interview_app.shared.enums.finance.PaymentStatus.PENDING
              and not exists (
                  select item
                  from InvoiceItem item
                  where item.commissionRecord.id = commission.id
                    and item.invoice.status <> co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus.CANCELLED
              )
            order by commission.createdAt asc
            """)
    List<CommissionRecord> findUnbilledByCompany(@Param("companyId") Long companyId);

    @Query("""
            select commission
            from CommissionRecord commission
            where commission.id in :ids
            """)
    List<CommissionRecord> findAllByIdIn(@Param("ids") Collection<Long> ids);
}
