package co.istad.ai_interview_app.features.seeker.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "portfolios")
public class Portfolio extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_seeker_profile_id", nullable = false)
    private JobSeekerProfile jobSeekerProfile;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(unique = true)
    private String publicUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VisibilityStatus visibility = VisibilityStatus.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileStatus status = ProfileStatus.ACTIVE;
}