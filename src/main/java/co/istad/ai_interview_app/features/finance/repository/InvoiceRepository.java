package co.istad.ai_interview_app.features.finance.repository;

import co.istad.ai_interview_app.features.finance.entity.Invoice;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Invoice> findAllByStatusOrderByCreatedAtDesc(InvoiceStatus status, Pageable pageable);

    Page<Invoice> findAllByCompany_IdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    /**
     * Recruiters read invoices through their own company, never by id alone —
     * pairing the two in the query is what stops one company reading another's
     * bill by guessing.
     */
    Optional<Invoice> findByIdAndCompany_Id(Long id, Long companyId);

    /**
     * Issued or part-paid invoices whose due date has passed.
     *
     * <p>Both statuses count: a company that paid half and then stopped is as
     * overdue as one that paid nothing.
     */
    @Query("""
            select invoice
            from Invoice invoice
            where invoice.status in (
                co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus.ISSUED,
                co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus.PARTIALLY_PAID
            )
              and invoice.dueAt is not null
              and invoice.dueAt < :now
            """)
    List<Invoice> findOverdue(@Param("now") Instant now);
}
