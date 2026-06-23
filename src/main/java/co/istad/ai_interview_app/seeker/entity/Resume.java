package co.istad.ai_interview_app.seeker.entity;

import co.istad.ai_interview_app.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
}