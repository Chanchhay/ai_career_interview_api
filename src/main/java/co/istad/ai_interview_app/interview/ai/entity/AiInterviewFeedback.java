package co.istad.ai_interview_app.interview.ai.entity;

import co.istad.ai_interview_app.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ai_interview_feedback")
public class AiInterviewFeedback extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "session_id", unique = true)
    private AiInterviewSession session;

    private BigDecimal communicationScore;

    private BigDecimal technicalScore;

    private BigDecimal confidenceScore;

    private BigDecimal problemSolvingScore;

    private BigDecimal overallScore;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String recommendation;
}