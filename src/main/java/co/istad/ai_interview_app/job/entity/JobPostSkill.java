package co.istad.ai_interview_app.job.entity;

import co.istad.ai_interview_app.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "job_post_skills",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"job_post_id", "skill_id"})
        }
)
public class JobPostSkill extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id")
    private JobPost jobPost;

    @ManyToOne(optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    private String requiredLevel;
}