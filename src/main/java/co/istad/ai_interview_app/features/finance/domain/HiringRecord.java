package co.istad.ai_interview_app.features.finance.domain;

import co.istad.ai_interview_app.features.job.domain.JobPost;
import co.istad.ai_interview_app.features.job_recruiter.domain.Company;
import co.istad.ai_interview_app.features.job_seeker.domain.JobApplication;
import co.istad.ai_interview_app.features.job_seeker.domain.JobSeekerProfile;
import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
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
}