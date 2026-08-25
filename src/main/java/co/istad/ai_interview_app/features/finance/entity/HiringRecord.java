package co.istad.ai_interview_app.features.finance.entity;

import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.finance.HiringRecordStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "hiring_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hiring_records_application",
                        columnNames = "application_id"
                )
        }
)
public class HiringRecord extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private JobApplication application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_seeker_profile_id", nullable = false)
    private JobSeekerProfile jobSeekerProfile;

    @Column(nullable = false)
    private Instant hiredAt = Instant.now();

    @Column(precision = 12, scale = 2)
    private BigDecimal offeredSalary;

    @Column(length = 10)
    private String salaryCurrency;

    @Column(columnDefinition = "TEXT")
    private String note;

    /**
     * Review state. A record exists from the moment the recruiter reports the
     * hire, so the claim is on file even if it is later rejected — but only
     * CONFIRMED produces a commission.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HiringRecordStatus status = HiringRecordStatus.REPORTED;

    @ManyToOne
    @JoinColumn(name = "reported_by_user_account_id")
    private UserAccount reportedByUserAccount;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_user_account_id")
    private UserAccount reviewedByUserAccount;

    private Instant reviewedAt;

    /** Why a report was rejected, or any note the moderator left on confirming. */
    @Column(columnDefinition = "TEXT")
    private String reviewNote;
}