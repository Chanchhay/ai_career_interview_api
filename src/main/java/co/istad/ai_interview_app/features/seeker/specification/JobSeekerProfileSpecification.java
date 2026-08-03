package co.istad.ai_interview_app.features.seeker.specification;

import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class JobSeekerProfileSpecification {

    public static Specification<JobSeekerProfile> filterPublicTalent(
            ProfileStatus status,
            VisibilityStatus visibility,
            String keyword,
            String preferredLocation,
            String availabilityStatus
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (visibility != null) {
                predicates.add(cb.equal(root.get("profileVisibility"), visibility));
            }

            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate headlineLike = cb.like(cb.lower(cb.coalesce(root.get("headline"), "")), likePattern);
                Predicate bioLike = cb.like(cb.lower(cb.coalesce(root.get("bio"), "")), likePattern);
                Predicate positionLike = cb.like(cb.lower(cb.coalesce(root.get("currentPosition"), "")), likePattern);
                predicates.add(cb.or(headlineLike, bioLike, positionLike));
            }

            if (preferredLocation != null && !preferredLocation.isBlank()) {
                String likePattern = "%" + preferredLocation.toLowerCase() + "%";
                Predicate locationLike = cb.like(cb.lower(cb.coalesce(root.get("preferredLocation"), "")), likePattern);
                predicates.add(locationLike);
            }

            if (availabilityStatus != null && !availabilityStatus.isBlank()) {
                Predicate availabilityEq = cb.equal(
                        cb.lower(cb.coalesce(root.get("availabilityStatus"), "")),
                        availabilityStatus.toLowerCase()
                );
                predicates.add(availabilityEq);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
