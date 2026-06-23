package co.istad.ai_interview_app.features.job.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.job.JobPostSectionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "job_post_sections")
public class JobPostSection extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_post_id")
    private JobPost jobPost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private JobPostSectionType sectionType;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentMarkdown;

    @Column(columnDefinition = "TEXT")
    private String contentText;

    @Column(nullable = false)
    private Integer displayOrder;
}