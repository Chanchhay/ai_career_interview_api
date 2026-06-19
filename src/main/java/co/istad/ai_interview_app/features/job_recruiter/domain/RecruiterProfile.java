package co.istad.ai_interview_app.features.job_recruiter.domain;

import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.domain.UserAccount;
import co.istad.ai_interview_app.shared.enums.ProfileStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "recruiter_profiles")
public class RecruiterProfile extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(length = 100)
    private String position;

    private String linkedinUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileStatus status = ProfileStatus.ACTIVE;
}