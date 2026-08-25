package co.istad.ai_interview_app.features.seeker.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.seeker.ResumeSourceType;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "resumes")
public class Resume extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_seeker_profile_id", nullable = false)
    private JobSeekerProfile jobSeekerProfile;

    @ManyToOne
    @JoinColumn(name = "template_id")
    private ResumeTemplate template;

    @Column(nullable = false, length = 150)
    private String title;

    private String resumeFileUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> resumeData;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VisibilityStatus visibility = VisibilityStatus.PRIVATE;

    private Instant publishedAt;

    /**
     * Whether this resume is rendered from {@link #resumeData} or is a file the
     * job seeker brought. Defaults to the template path because that is what the
     * builder produces, and every resume created before this column existed came
     * from the builder.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private ResumeSourceType sourceType = ResumeSourceType.PLATFORM_TEMPLATE;

    /** When the current {@link #resumeFileUrl} was rendered. Null for uploads. */
    private Instant generatedAt;

    /**
     * Bumped on every regeneration.
     *
     * <p>Exists so a stale link is recognisable as stale: a resume attached to
     * an application records the version that was sent, and comparing it with
     * this tells you whether the candidate has edited it since.
     */
    @Column(nullable = false)
    private Integer fileVersion = 0;
}
