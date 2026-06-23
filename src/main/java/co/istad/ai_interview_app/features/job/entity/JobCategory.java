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

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}