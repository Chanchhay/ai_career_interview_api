package co.istad.ai_interview_app.features.job_seeker.domain.ai;

import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ai_interview_answers")
public class AiInterviewAnswer extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AiInterviewQuestion question;

    @Column(columnDefinition = "TEXT")
    private String answerText;

    private String audioUrl;

    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}