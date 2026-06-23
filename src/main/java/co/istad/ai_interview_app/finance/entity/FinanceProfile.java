package co.istad.ai_interview_app.finance.entity;

import co.istad.ai_interview_app.common.audit.BaseEntity;
import co.istad.ai_interview_app.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.ProfileStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "finance_profiles")
public class FinanceProfile extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(length = 100)
    private String position;

    @Column(length = 50)
    private String approvalLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileStatus status = ProfileStatus.ACTIVE;
}