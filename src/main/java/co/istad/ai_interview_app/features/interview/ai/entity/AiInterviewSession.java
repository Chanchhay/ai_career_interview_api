package co.istad.ai_interview_app.features.interview.ai.entity;

import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.InterviewResult;
import co.istad.ai_interview_app.shared.enums.InterviewStatus;
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
@Table(name = "ai_interview_sessions")
public class AiInterviewSession extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id")
    private JobApplication application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id")
    private JobPost jobPost;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_seeker_id")
    private UserAccount jobSeeker;

    private String provider;

    private String aiModel;

    private String callSessionId;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status = InterviewStatus.PENDING;

    private Instant startedAt;

    private Instant endedAt;

    private BigDecimal totalScore;

    @Enumerated(EnumType.STRING)
    private InterviewResult result;

    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AiInterviewQuestion> questions = new ArrayList<>();
}