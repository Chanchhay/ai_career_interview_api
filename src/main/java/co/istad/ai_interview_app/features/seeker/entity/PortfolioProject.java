package co.istad.ai_interview_app.features.seeker.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "portfolio_projects")
public class PortfolioProject extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String projectUrl;

    private String githubUrl;

    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String techStack;

    private Integer displayOrder;
}