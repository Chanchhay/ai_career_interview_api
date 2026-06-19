package co.istad.ai_interview_app.features.moderator.domain;

import co.istad.ai_interview_app.features.job.domain.JobPost;
import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.ModerationDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "job_moderation_logs")
public class JobModerationLog extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moderator_profile_id", nullable = false)
    private ModeratorProfile moderatorProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ModerationDecision action;

    @Column(columnDefinition = "TEXT")
    private String note;
}