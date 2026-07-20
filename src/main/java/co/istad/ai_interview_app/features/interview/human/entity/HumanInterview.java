package co.istad.ai_interview_app.features.interview.human.entity;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.moderator.entity.ModeratorProfile;
import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "human_interviews")
public class HumanInterview extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moderator_profile_id", nullable = false)
    private ModeratorProfile moderator;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Column(nullable = false, length = 500)
    private String meetingUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InterviewStatus status = InterviewStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private InterviewResult result;

    @Column(columnDefinition = "TEXT")
    private String note;

    private Instant completedAt;

    private Instant cancelledAt;
}
