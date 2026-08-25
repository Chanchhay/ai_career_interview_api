package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.FavoriteJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteJobRepository extends JpaRepository<FavoriteJob, Long> {

    /**
     * The saved-jobs page. Fetches the company eagerly because every row on
     * that page prints the company name, and a lazy load per row would turn one
     * page into twenty-one queries.
     */
    @Query(
            value = """
                    select favoriteJob
                    from FavoriteJob favoriteJob
                    join fetch favoriteJob.jobPost jobPost
                    join fetch jobPost.company
                    where favoriteJob.jobSeekerProfile.id = :jobSeekerProfileId
                    """,
            countQuery = """
                    select count(favoriteJob)
                    from FavoriteJob favoriteJob
                    where favoriteJob.jobSeekerProfile.id = :jobSeekerProfileId
                    """
    )
    Page<FavoriteJob> findPageByJobSeekerProfileId(
            @Param("jobSeekerProfileId") Long jobSeekerProfileId,
            Pageable pageable
    );

    Optional<FavoriteJob> findByJobSeekerProfile_IdAndJobPost_Id(Long jobSeekerProfileId, Long jobPostId);

    /**
     * Which of {@code jobPostIds} the caller has saved, for the {@code isFavorite}
     * flag on the public job responses. Keyed by Keycloak subject rather than
     * profile id so the public path never has to resolve a seeker profile — a
     * recruiter or an admin browsing jobs simply matches nothing.
     */
    @Query("""
            select favoriteJob.jobPost.id
            from FavoriteJob favoriteJob
            where favoriteJob.jobSeekerProfile.userAccount.keycloakUserId = :keycloakUserId
              and favoriteJob.jobPost.id in :jobPostIds
            """)
    List<Long> findSavedJobPostIds(
            @Param("keycloakUserId") String keycloakUserId,
            @Param("jobPostIds") Collection<Long> jobPostIds
    );
}
