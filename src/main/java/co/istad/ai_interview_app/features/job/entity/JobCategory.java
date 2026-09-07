package co.istad.ai_interview_app.features.job.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "job_categories")
public class JobCategory extends BaseEntity {

    /** Null for a parent category; subcategories point directly to a parent. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_job_categories_parent"))
    private JobCategory parent;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}