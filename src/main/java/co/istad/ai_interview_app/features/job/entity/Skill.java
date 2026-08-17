package co.istad.ai_interview_app.features.job.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "skills")
public class Skill extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String skillType;

    /**
     * The recruiter who added this skill, or null when an admin entered it.
     *
     * <p>Recruiters may name skills the curated list is missing, and those rows
     * are live immediately. This is what lets an admin tell one apart from an
     * entry they made themselves, and find it to review.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_recruiter_profile_id")
    private RecruiterProfile createdByRecruiterProfile;
}