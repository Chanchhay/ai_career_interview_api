package co.istad.ai_interview_app.features.finance.repository;

import co.istad.ai_interview_app.features.finance.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findAllByInvoice_IdOrderByIdAsc(Long invoiceId);

    /**
     * The live invoice line for a commission, if one exists.
     *
     * <p>Excludes cancelled invoices, which is what returns a commission to the
     * unbilled pool: cancelling a bill should make its commissions billable
     * again rather than stranding them.
     */
    Optional<InvoiceItem> findFirstByCommissionRecord_IdAndInvoice_StatusNot(
            Long commissionRecordId,
            InvoiceStatus excludedStatus
    );
}
