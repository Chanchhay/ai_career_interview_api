package co.istad.ai_interview_app.features.job.entity;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
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

    /**
     * What this job's hand-written interview questions do to AI generation.
     *
     * <p>Ignored entirely while the job has no written questions, so the default
     * only starts to matter once somebody writes one — and then it keeps the
     * interview its usual length rather than shrinking it to what was written.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50,
            /*
             * The database default is spelled out so that adding this column to
             * a table that already holds rows works. Without it, ddl-auto emits
             * ADD COLUMN ... NOT NULL with nothing to put in the existing rows,
             * and the server will not start. Flyway's V21 does the same thing
             * properly; this is what keeps a boot that ran before V21 — or
             * without Flyway at all — from failing.
             */
            columnDefinition = "varchar(50) default 'MANUAL_PLUS_AI'"
    )
    private ManualQuestionMode manualQuestionMode = ManualQuestionMode.MANUAL_PLUS_AI;

    private Instant publishedAt;

    private Instant expiredAt;

    /**
     * App-relative URL of the PDF job description this post was parsed from,
     * when the recruiter created it by upload. Private: never mapped onto the
     * public job responses.
     */
    @Column(length = 500)
    private String sourceFileUrl;

    @OneToMany(
            mappedBy = "jobPost",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<JobPostSection> sections = new ArrayList<>();

    @OneToMany(
            mappedBy = "jobPost",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<JobPostSkill> skills = new ArrayList<>();
}
