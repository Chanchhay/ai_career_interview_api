package co.istad.ai_interview_app.features.interview.question.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A question an administrator wrote for a job, kept ready for every candidate
 * who interviews for it.
 *
 * <p>Distinct from {@code AiInterviewQuestion}, which belongs to one session and
 * is what a single candidate was actually asked. This is the template; a session
 * copies from it. Keeping them apart means editing the bank cannot rewrite the
 * transcript of an interview that already happened.
 */
@Getter
@Setter
@Entity
@Table(name = "job_interview_questions")
public class JobInterviewQuestion extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private InterviewQuestionType questionType;

    /** What a good answer covers. Handed to the AI as the scoring rubric. */
    @Column(columnDefinition = "TEXT")
    private String expectedAnswer;

    @Column(nullable = false)
    private Integer maxScore = 10;

    /** Position in the interview, 1-based and contiguous within a job. */
    @Column(nullable = false)
    private Integer displayOrder = 1;
}
