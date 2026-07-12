package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, Long> {

    Optional<JobSeekerProfile> findByUserAccount_KeycloakUserId(String keycloakUserId);

    Optional<JobSeekerProfile> findByPublicProfileSlugAndStatusAndProfileVisibility(
            String publicProfileSlug,
            ProfileStatus status,
            VisibilityStatus profileVisibility
    );

    @Query("""
            select profile
            from JobSeekerProfile profile
            where profile.status = :status
              and profile.profileVisibility = :visibility
              and (
                    :keyword is null
                    or lower(coalesce(profile.headline, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(profile.bio, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(profile.currentPosition, '')) like lower(concat('%', :keyword, '%'))
              )
              and (
                    :preferredLocation is null
                    or lower(coalesce(profile.preferredLocation, '')) like lower(concat('%', :preferredLocation, '%'))
              )
              and (
                    :availabilityStatus is null
                    or lower(coalesce(profile.availabilityStatus, '')) = lower(:availabilityStatus)
              )
            """)
    Page<JobSeekerProfile> findPublicTalent(
            ProfileStatus status,
            VisibilityStatus visibility,
            String keyword,
            String preferredLocation,
            String availabilityStatus,
            Pageable pageable
    );
}
