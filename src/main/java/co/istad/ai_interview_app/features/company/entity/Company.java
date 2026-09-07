package co.istad.ai_interview_app.features.company.entity;

import co.istad.ai_interview_app.features.common.audit.BaseEntity;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
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

    /**
     * Whether candidates are told who this company is.
     *
     * <p>The database default is spelled out so this column can be added to a
     * table that already holds companies: without it, ddl-auto would emit ADD
     * COLUMN ... NOT NULL with nothing to put in the existing rows.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50,
            columnDefinition = "varchar(50) default 'VISIBLE'"
    )
    private CompanyIdentityVisibility identityVisibility = CompanyIdentityVisibility.VISIBLE;

    /**
     * A stand-in logo shown to candidates while this company is masked.
     *
     * <p>Set by an administrator, never by the recruiter: the point of masking
     * is that the company does not choose what candidates see of it. Null is
     * ordinary and means a masked posting simply carries no mark.
     *
     * <p>Whoever sets this is responsible for it being neutral. A stand-in that
     * is recognisably the company's own branding unmasks it just as thoroughly
     * as leaking {@link #logoUrl} would.
     */
    @Column(length = 500)
    private String maskedLogoUrl;
}
