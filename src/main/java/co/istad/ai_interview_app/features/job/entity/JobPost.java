package co.istad.ai_interview_app.features.job.entity;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "job_posts")
public class JobPost extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recruiter_profile_id", nullable = false)
    private RecruiterProfile recruiterProfile;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private JobCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String location;

    private String jobType;

    private String workMode;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private JobStatus status = JobStatus.DRAFT;

    private Instant publishedAt;

    private Instant expiredAt;

    @OneToMany(
            mappedBy = "jobPost",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<JobPostSection> sections = new ArrayList<>();
}