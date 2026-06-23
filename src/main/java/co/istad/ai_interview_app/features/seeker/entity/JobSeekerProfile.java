package co.istad.ai_interview_app.features.seeker.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.profile.SalaryVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "job_seeker_profiles")
public class JobSeekerProfile extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(length = 255)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 150)
    private String currentPosition;

    private BigDecimal expectedSalaryMin;

    private BigDecimal expectedSalaryMax;

    @Column(length = 10)
    private String expectedSalaryCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SalaryVisibility salaryVisibility = SalaryVisibility.PRIVATE;

    @Column(length = 150)
    private String preferredLocation;

    @Column(length = 50)
    private String availabilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileStatus status = ProfileStatus.ACTIVE;
}