package co.istad.ai_interview_app.features.job.specification;

import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JobPostSpecification {

    public static Specification<JobPost> filterPublicJobs(
            JobStatus status,
            VerificationStatus verificationStatus,
            ProfileStatus companyStatus,
            Instant now,
            String keyword,
            String location,
            Long categoryId,
            List<Long> skillIds,
            String workMode,
            String jobType
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // company.verificationStatus and company.status
            Join<Object, Object> company = root.join("company", JoinType.INNER);
            if (verificationStatus != null) {
                predicates.add(cb.equal(company.get("verificationStatus"), verificationStatus));
            }
            if (companyStatus != null) {
                predicates.add(cb.equal(company.get("status"), companyStatus));
            }

            // expiredAt is null or expiredAt > now
            if (now != null) {
                Predicate expiredAtNull = cb.isNull(root.get("expiredAt"));
                Predicate expiredAtFuture = cb.greaterThan(root.get("expiredAt"), now);
                predicates.add(cb.or(expiredAtNull, expiredAtFuture));
            }

            // keyword
            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), likePattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), likePattern);
                /*
                 * A masked company's name is not searchable. Matching on it
                 * would hand the name straight back: type "Acme", get Acme's
                 * confidential postings, and the mask on the listing itself
                 * stops meaning anything.
                 */
                Predicate companyNameLike = cb.and(
                        cb.equal(
                                company.get("identityVisibility"),
                                CompanyIdentityVisibility.VISIBLE
                        ),
                        cb.like(cb.lower(company.get("name")), likePattern)
                );
                predicates.add(cb.or(titleLike, descLike, companyNameLike));
            }

            // location
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            }

            // categoryId
            if (categoryId != null) {
                Join<Object, Object> category = root.join("category", JoinType.LEFT);
                predicates.add(cb.equal(category.get("id"), categoryId));
            }

            // skillIds
            if (skillIds != null && !skillIds.isEmpty()) {
                Join<Object, Object> skills = root.join("skills", JoinType.LEFT);
                Join<Object, Object> skill = skills.join("skill", JoinType.LEFT);
                predicates.add(skill.get("id").in(skillIds));
            }

            // workMode
            if (workMode != null && !workMode.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("workMode")), workMode.toLowerCase()));
            }

            // jobType
            if (jobType != null && !jobType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("jobType")), jobType.toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
