package co.istad.ai_interview_app.features.job_recruiter.domain;

import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.enums.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "recruiter_profile_id", nullable = false)
    private RecruiterProfile recruiterProfile;

    @ManyToOne
    @JoinColumn(name = "industry_id")
    private Industry industry;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String websiteUrl;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String contactEmail;

    private String contactPhone;

    private String logoUrl;

    @Column(unique = true, length = 100)
    private String businessRegistrationNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileStatus status = ProfileStatus.PENDING;
}