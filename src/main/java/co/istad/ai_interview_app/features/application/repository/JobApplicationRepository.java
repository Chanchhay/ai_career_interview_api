package co.istad.ai_interview_app.features.application.repository;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /**
     * Whether the seeker already has a live application for this job.
     *
     * <p>Closed attempts do not count, which is what makes re-applying after a
     * rejection possible. The database enforces the same rule through a partial
     * unique index, so a race that slips past this check still cannot create a
     * second live row.
     */
    @Query("""
            select case when count(application) > 0 then true else false end
            from JobApplication application
            where application.jobPost.id = :jobPostId
              and application.jobSeekerProfile.id = :jobSeekerProfileId
              and application.status not in (
                  co.istad.ai_interview_app.shared.enums.application.ApplicationStatus.REJECTED,
                  co.istad.ai_interview_app.shared.enums.application.ApplicationStatus.WITHDRAWN
              )
            """)
    boolean existsLiveApplication(
            @Param("jobPostId") Long jobPostId,
            @Param("jobSeekerProfileId") Long jobSeekerProfileId
    );

    /**
     * The seeker's live application for a job, if any.
     *
     * <p>Used to attach AI interviews. Deliberately ignores closed attempts: a
     * practice interview run today belongs to the application the candidate is
     * pursuing now, not to one that was rejected last month.
     */
    @Query("""
            select application
            from JobApplication application
            where application.jobPost.id = :jobPostId
              and application.jobSeekerProfile.id = :jobSeekerProfileId
              and application.status not in (
                  co.istad.ai_interview_app.shared.enums.application.ApplicationStatus.REJECTED,
                  co.istad.ai_interview_app.shared.enums.application.ApplicationStatus.WITHDRAWN
              )
            """)
    Optional<JobApplication> findLiveApplication(
            @Param("jobPostId") Long jobPostId,
            @Param("jobSeekerProfileId") Long jobSeekerProfileId
    );

    /**
     * The candidate's most recent rejection for this job, if any.
     *
     * <p>Only rejections: a withdrawal is the candidate's own decision, and
     * making them wait out a cooldown for changing their mind would punish the
     * one closing action that is not a judgement about them.
     */
    @Query("""
            select application
            from JobApplication application
            where application.jobPost.id = :jobPostId
              and application.jobSeekerProfile.id = :jobSeekerProfileId
              and application.status = co.istad.ai_interview_app.shared.enums.application.ApplicationStatus.REJECTED
            order by application.closedAt desc nulls last, application.id desc
            """)
    List<JobApplication> findRejectedApplicationsNewestFirst(
            @Param("jobPostId") Long jobPostId,
            @Param("jobSeekerProfileId") Long jobSeekerProfileId
    );

    boolean existsByResume_Id(Long resumeId);

    @EntityGraph(attributePaths = {"jobPost", "resume", "jobSeekerProfile", "jobSeekerProfile.userAccount"})
    List<JobApplication> findAllByJobSeekerProfile_UserAccount_KeycloakUserIdOrderByAppliedAtDesc(String keycloakUserId);

    @EntityGraph(attributePaths = {"jobPost", "resume", "jobSeekerProfile", "jobSeekerProfile.userAccount"})
    Optional<JobApplication> findByIdAndJobSeekerProfile_UserAccount_KeycloakUserId(Long id, String keycloakUserId);

    @EntityGraph(attributePaths = {"jobPost", "resume", "jobSeekerProfile", "jobSeekerProfile.userAccount"})
    Optional<JobApplication> findByIdAndJobPost_RecruiterProfile_UserAccount_KeycloakUserId(Long id, String keycloakUserId);

    @EntityGraph(attributePaths = {"jobPost", "resume", "jobSeekerProfile", "jobSeekerProfile.userAccount"})
    List<JobApplication> findAllByStatus(ApplicationStatus status);
}
