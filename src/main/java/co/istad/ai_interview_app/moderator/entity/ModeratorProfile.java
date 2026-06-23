package co.istad.ai_interview_app.moderator.entity;

import co.istad.ai_interview_app.common.audit.BaseEntity;
import co.istad.ai_interview_app.identity.entity.UserAccount;
import co.istad.ai_interview_app.shared.enums.ProfileStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "moderator_profiles")
public class ModeratorProfile extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(length = 100)
    private String specialization;

    @Column(length = 50)
    private String verificationLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileStatus status = ProfileStatus.ACTIVE;
}