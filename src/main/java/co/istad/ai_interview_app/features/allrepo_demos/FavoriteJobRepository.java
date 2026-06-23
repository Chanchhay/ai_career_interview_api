package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.seeker.entity.FavoriteJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface FavoriteJobRepository extends JpaRepository<FavoriteJob, Long> {

    List<FavoriteJob> findByJobSeekerProfile_Id(Long jobSeekerProfileId);

    Optional<FavoriteJob> findByJobSeekerProfile_IdAndJobPost_Id(
            Long jobSeekerProfileId,
            Long jobPostId
    );

    boolean existsByJobSeekerProfile_IdAndJobPost_Id(
            Long jobSeekerProfileId,
            Long jobPostId
    );
}