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

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long>, JpaSpecificationExecutor<JobPost> {

    List<JobPost> findAllByRecruiterProfile_UserAccount_KeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    Optional<JobPost> findByIdAndRecruiterProfile_UserAccount_KeycloakUserId(Long id, String keycloakUserId);

    Optional<JobPost> findByIdAndStatus(Long id, JobStatus status);



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
