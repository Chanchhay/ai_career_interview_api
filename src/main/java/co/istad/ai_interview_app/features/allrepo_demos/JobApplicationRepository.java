package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job_seeker.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByJobSeekerProfile_Id(Long jobSeekerProfileId);

    List<JobApplication> findByJobPost_Id(Long jobPostId);

    Optional<JobApplication> findByJobPost_IdAndJobSeekerProfile_Id(
            Long jobPostId,
            Long jobSeekerProfileId
    );

    boolean existsByJobPost_IdAndJobSeekerProfile_Id(
            Long jobPostId,
            Long jobSeekerProfileId
    );
}