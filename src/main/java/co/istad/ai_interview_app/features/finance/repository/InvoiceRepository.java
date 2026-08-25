package co.istad.ai_interview_app.features.finance.repository;

import co.istad.ai_interview_app.features.finance.entity.Invoice;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
