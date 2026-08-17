package co.istad.ai_interview_app.features.moderator.entity;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "candidate_application_reviews")
public class CandidateApplicationReview extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private JobApplication application;

    @ManyToOne
    @JoinColumn(name = "moderator_profile_id")
    private ModeratorProfile moderator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private CandidateApplicationReviewStatus reviewStatus = CandidateApplicationReviewStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String decisionNote;

    private Instant reviewedAt;

    private Instant approvedAt;

    private Instant forwardedAt;
}
