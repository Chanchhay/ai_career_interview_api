package co.istad.ai_interview_app.features.job.repository;

import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    List<JobPost> findAllByRecruiterProfile_UserAccount_KeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    Optional<JobPost> findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(Long id, String keycloakUserId);

    Optional<JobPost> findByIdAndStatus(Long id, JobStatus status);

    @Query(
            value = """
                    select distinct job
                    from JobPost job
                    left join job.skills jobSkill
                    where job.status = :status
                      and job.company.verificationStatus = :verificationStatus
                      and job.company.status = :companyStatus
                      and (job.expiredAt is null or job.expiredAt > :now)
                      and (
                        :keyword is null
                        or lower(job.title) like lower(concat('%', :keyword, '%'))
                        or lower(job.description) like lower(concat('%', :keyword, '%'))
                        or lower(job.company.name) like lower(concat('%', :keyword, '%'))
                      )
                      and (:location is null or lower(job.location) like lower(concat('%', :location, '%')))
                      and (:categoryId is null or job.category.id = :categoryId)
                      and (:skillCount = 0 or jobSkill.skill.id in :skillIds)
                      and (:workMode is null or lower(job.workMode) = lower(:workMode))
                      and (:jobType is null or lower(job.jobType) = lower(:jobType))
                    """,
            countQuery = """
                    select count(distinct job)
                    from JobPost job
                    left join job.skills jobSkill
                    where job.status = :status
                      and job.company.verificationStatus = :verificationStatus
                      and job.company.status = :companyStatus
                      and (job.expiredAt is null or job.expiredAt > :now)
                      and (
                        :keyword is null
                        or lower(job.title) like lower(concat('%', :keyword, '%'))
                        or lower(job.description) like lower(concat('%', :keyword, '%'))
                        or lower(job.company.name) like lower(concat('%', :keyword, '%'))
                      )
                      and (:location is null or lower(job.location) like lower(concat('%', :location, '%')))
                      and (:categoryId is null or job.category.id = :categoryId)
                      and (:skillCount = 0 or jobSkill.skill.id in :skillIds)
                      and (:workMode is null or lower(job.workMode) = lower(:workMode))
                      and (:jobType is null or lower(job.jobType) = lower(:jobType))
                    """
    )
    Page<JobPost> findPublicJobs(
            @Param("status") JobStatus status,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("companyStatus") ProfileStatus companyStatus,
            @Param("now") Instant now,
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("categoryId") Long categoryId,
            @Param("skillIds") List<Long> skillIds,
            @Param("skillCount") int skillCount,
            @Param("workMode") String workMode,
            @Param("jobType") String jobType,
            Pageable pageable
    );

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
            @Param("id") Long id,
            @Param("status") JobStatus status,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("companyStatus") ProfileStatus companyStatus,
            @Param("now") Instant now
    );
}
