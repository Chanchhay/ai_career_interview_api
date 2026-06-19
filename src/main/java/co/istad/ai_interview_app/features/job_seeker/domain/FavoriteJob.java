package co.istad.ai_interview_app.features.job_seeker.domain;

import co.istad.ai_interview_app.features.job.domain.JobPost;
import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "favorite_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_favorite_jobs_profile_job",
                        columnNames = {"job_seeker_profile_id", "job_post_id"}
                )
        }
)
public class FavoriteJob extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_seeker_profile_id", nullable = false)
    private JobSeekerProfile jobSeekerProfile;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;
}