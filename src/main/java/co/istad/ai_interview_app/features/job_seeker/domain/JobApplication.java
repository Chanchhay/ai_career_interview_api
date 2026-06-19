package co.istad.ai_interview_app.features.job_seeker.domain;

import co.istad.ai_interview_app.features.job.domain.JobPost;
import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "job_applications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_applications_job_profile",
                        columnNames = {"job_post_id", "job_seeker_profile_id"}
                )
        }
)
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
}