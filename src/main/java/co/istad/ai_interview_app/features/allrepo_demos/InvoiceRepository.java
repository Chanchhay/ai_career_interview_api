package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.finance.entity.Invoice;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNo(String invoiceNo);

    List<Invoice> findByCompany_Id(Long companyId);

    List<Invoice> findByFinanceProfile_Id(Long financeProfileId);

    List<Invoice> findByStatus(InvoiceStatus status);
}