package co.istad.ai_interview_app.features.application.entity;

import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
/*
 * No uniqueConstraints here on purpose.
 *
 * A candidate may hold only one *live* application per job, but any number of
 * closed ones — that is what lets someone re-apply after being rejected while
 * keeping the earlier attempt on record. That is a partial unique index, which
 * JPA cannot express, so it lives in
 * V19__allow_reapplying_after_a_closed_application.sql instead. Declaring a
 * plain unique constraint here would make ddl-auto recreate the very rule the
 * migration removes.
 */
@Table(name = "job_applications")
public class JobApplication extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_seeker_profile_id", nullable = false)
    private JobSeekerProfile jobSeekerProfile;

    @ManyToOne
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    private BigDecimal matchScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(nullable = false)
    private Instant appliedAt = Instant.now();

    /**
     * When the application was closed — rejected or withdrawn.
     *
     * <p>The re-apply cooldown counts from here rather than from
     * {@code updatedAt}, which moves whenever anything on the row changes and
     * would silently restart the clock.
     */
    private Instant closedAt;
}
