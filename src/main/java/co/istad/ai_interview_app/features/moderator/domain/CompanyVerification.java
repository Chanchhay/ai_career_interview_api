package co.istad.ai_interview_app.features.moderator.domain;

import co.istad.ai_interview_app.features.job_recruiter.domain.Company;
import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.ModerationDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "company_verifications")
public class CompanyVerification extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moderator_profile_id", nullable = false)
    private ModeratorProfile moderatorProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ModerationDecision decision;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private Instant verifiedAt = Instant.now();
}