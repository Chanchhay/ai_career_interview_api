package co.istad.ai_interview_app.features.moderator.domain;

import co.istad.ai_interview_app.features.job_seeker.domain.JobApplication;
import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "project_assignments")
public class ProjectAssignment extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by_moderator_profile_id", nullable = false)
    private ModeratorProfile assignedByModeratorProfile;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Instant deadlineAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectStatus status = ProjectStatus.ASSIGNED;
}