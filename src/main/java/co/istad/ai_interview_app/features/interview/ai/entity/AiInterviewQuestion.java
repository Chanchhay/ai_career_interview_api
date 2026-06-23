package co.istad.ai_interview_app.features.interview.ai.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ai_interview_questions")
public class AiInterviewQuestion extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AiInterviewSession session;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(length = 80)
    private InterviewQuestionType questionType;

    @Column(columnDefinition = "TEXT")
    private String expectedAnswer;

    private Integer displayOrder;

    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AiInterviewAnswer> answers = new ArrayList<>();
}
