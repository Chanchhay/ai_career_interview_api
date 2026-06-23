package co.istad.ai_interview_app.finance.entity;

import co.istad.ai_interview_app.company.entity.Company;
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
@Table(
        name = "commission_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_commission_records_hiring_record",
                        columnNames = "hiring_record_id"
                )
        }
)
public class CommissionRecord extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "hiring_record_id", nullable = false, unique = true)
    private HiringRecord hiringRecord;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    private Instant dueAt;

    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;
}