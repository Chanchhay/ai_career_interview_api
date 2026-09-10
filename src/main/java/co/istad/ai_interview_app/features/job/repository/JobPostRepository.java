package co.istad.ai_interview_app.features.job.repository;

import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, UUID>, JpaSpecificationExecutor<JobPost> {

    List<JobPost> findAllByRecruiterProfile_UserAccount_KeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    Optional<JobPost> findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(UUID id, String keycloakUserId);

    Optional<JobPost> findByIdAndStatus(UUID id, JobStatus status);

    /**
     * Job counts for a page of companies: total, and how many are live.
     *
     * <p>One query for the whole page rather than a count per row — the
     * companies queue renders twenty at a time, and twenty extra round trips to
     * print a number beside each name is not worth it.
     */
    @Query("""
            select job.company.id, count(job),
                   sum(case when job.status = :published then 1 else 0 end)
            from JobPost job
            where job.company.id in :companyIds
            group by job.company.id
            """)
    List<Object[]> countByCompanyIds(
            @Param("companyIds") List<UUID> companyIds,
            @Param("published") JobStatus published
    );

    /** Every job a company has, whatever its state — the moderator's view. */
    Page<JobPost> findAllByCompany_IdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);



    @Query("""
            select job
            from JobPost job
            where job.id = :id
              and job.status = :status
              and job.company.verificationStatus = :verificationStatus
              and job.company.status = :companyStatus
              and (job.expiredAt is null or job.expiredAt > :now)
            """)
    Optional<JobPost> findPublicJobById(
            @Param("id") UUID id,
            @Param("status") JobStatus status,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("companyStatus") ProfileStatus companyStatus,
            @Param("now") Instant now
    );

    /**
     * The same visibility test as {@link #findPublicJobById}, for many ids at
     * once. Ids that are not public are simply absent from the result rather
     * than an error — a caller holding a stale list should get the jobs that
     * are still on the board, not a failure.
     */
    @Query("""
            select job
            from JobPost job
            where job.id in :ids
              and job.status = :status
              and job.company.verificationStatus = :verificationStatus
              and job.company.status = :companyStatus
              and (job.expiredAt is null or job.expiredAt > :now)
            """)
    List<JobPost> findPublicJobsByIds(
            @Param("ids") List<UUID> ids,
            @Param("status") JobStatus status,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("companyStatus") ProfileStatus companyStatus,
            @Param("now") Instant now
    );
}
