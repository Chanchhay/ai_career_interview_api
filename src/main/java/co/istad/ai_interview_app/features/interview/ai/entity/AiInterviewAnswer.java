package co.istad.ai_interview_app.features.interview.ai.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
        name = "ai_interview_answers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_interview_answers_question",
                        columnNames = "question_id"
                )
        }
)
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

    /**
     * What a strong answer would have said. Written when the interview is
     * scored, so it stays null while the interview is still being taken.
     */
    @Column(columnDefinition = "TEXT")
    private String modelAnswer;
}
