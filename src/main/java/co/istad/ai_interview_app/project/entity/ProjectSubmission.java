package co.istad.ai_interview_app.project.entity;

import co.istad.ai_interview_app.common.audit.BaseEntity;
import co.istad.ai_interview_app.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "project_submissions")
public class ProjectSubmission extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_assignment_id", nullable = false)
    private ProjectAssignment projectAssignment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_seeker_profile_id", nullable = false)
    private JobSeekerProfile jobSeekerProfile;

    private String submissionUrl;

    private String githubUrl;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private Instant submittedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectStatus status = ProjectStatus.SUBMITTED;
}
