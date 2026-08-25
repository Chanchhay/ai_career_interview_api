package co.istad.ai_interview_app.features.finance.repository;

import co.istad.ai_interview_app.features.finance.entity.InvoicePayment;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {

    List<InvoicePayment> findAllByInvoice_IdOrderByIdAsc(Long invoiceId);

    /**
     * What the invoice has actually been paid.
     *
     * <p>Summed from the payment rows rather than read from
     * {@code invoice.paidAmount}: that column is a cached total, and recomputing
     * it from its source on every change is what keeps the two honest.
     */
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(payment.amount), 0)
            from InvoicePayment payment
            where payment.invoice.id = :invoiceId
              and payment.status = :status
            """)
    Optional<BigDecimal> sumAmountByInvoiceAndStatus(
            @org.springframework.data.repository.query.Param("invoiceId") Long invoiceId,
            @org.springframework.data.repository.query.Param("status") PaymentStatus status
    );
}
