package co.istad.ai_interview_app.features.interview.ai.entity;

import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;
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

    @ManyToOne
    @JoinColumn(name = "application_id")
    private JobApplication application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id")
    private JobPost jobPost;

    /**
     * The signed-in candidate, or null for a guest interview.
     *
     * <p>Nullable so a guest can sit an interview without an account. Every
     * candidate-facing query filters on this or on the application, so a guest
     * session cannot appear in a seeker's history, in a moderator's queue, or in
     * the approval gate — none of which would match a null owner.
     */
    @ManyToOne
    @JoinColumn(name = "job_seeker_id")
    private UserAccount jobSeeker;

    /**
     * Identifies the browser that owns a guest interview, and is the only thing
     * authorising reads and writes on it. Treated as a bearer secret: whoever
     * holds it is that guest.
     */
    @Column(length = 64)
    private String guestToken;

    /**
     * A salted hash of the guest's IP, used only to count interviews per
     * network per day. Hashed rather than stored, because the address is not
     * needed for anything except that count.
     */
    @Column(length = 64)
    private String guestIpHash;

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

    @OneToOne(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private AiInterviewFeedback feedback;
}
