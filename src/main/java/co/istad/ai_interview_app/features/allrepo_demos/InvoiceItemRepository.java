package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.finance.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoice_Id(Long invoiceId);

    List<InvoiceItem> findByCommissionRecord_Id(Long commissionRecordId);
}