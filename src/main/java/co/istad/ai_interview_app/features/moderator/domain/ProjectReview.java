package co.istad.ai_interview_app.features.moderator.domain;

import co.istad.ai_interview_app.features.job_seeker.domain.ProjectSubmission;
import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.domain.UserAccount;
import co.istad.ai_interview_app.shared.enums.InterviewResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "project_reviews")
public class ProjectReview extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_submission_id", nullable = false)
    private ProjectSubmission projectSubmission;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reviewer_user_account_id", nullable = false)
    private UserAccount reviewerUserAccount;

    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private InterviewResult decision;

    @Column(nullable = false)
    private Instant reviewedAt = Instant.now();
}