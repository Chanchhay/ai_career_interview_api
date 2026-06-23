package co.istad.ai_interview_app.finance.entity;

import co.istad.ai_interview_app.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "invoice_payments")
public class InvoicePayment extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(length = 80)
    private String paymentMethod;

    @Column(length = 150)
    private String transactionReference;

    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;
}