package co.istad.ai_interview_app.features.job.specification;

import co.istad.ai_interview_app.features.job.dto.PublicJobFilter;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JobPostSpecification {

    private static final Set<String> SALARY_PROPERTIES = Set.of("salaryMin", "salaryMax");

    /*
     * Stand-ins for "this job names no salary", chosen to sit outside any real
     * one so that such a job lands at the end of the listing whichever way the
     * salary is sorted. numeric(38,2) holds both comfortably.
     */
    private static final BigDecimal BELOW_ANY_SALARY = new BigDecimal("-1");
    private static final BigDecimal ABOVE_ANY_SALARY = new BigDecimal("1000000000000");

    public static Specification<JobPost> filterPublicJobs(
            JobStatus status,
            VerificationStatus verificationStatus,
            ProfileStatus companyStatus,
            Instant now,
            PublicJobFilter filter
    ) {
        return (root, query, cb) -> {
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
            if (hasText(filter.keyword())) {
                String likePattern = "%" + filter.keyword().toLowerCase() + "%";
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
            if (hasText(filter.location())) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + filter.location().toLowerCase() + "%"));
            }

            // categoryIds
            if (hasValues(filter.categoryIds())) {
                Join<Object, Object> category = root.join("category", JoinType.LEFT);
                predicates.add(category.get("id").in(filter.categoryIds()));
            }

            // skillIds - a job matching any one of the requested skills is a hit
            if (hasValues(filter.skillIds())) {
                Join<Object, Object> skills = root.join("skills", JoinType.LEFT);
                Join<Object, Object> skill = skills.join("skill", JoinType.LEFT);
                predicates.add(skill.get("id").in(filter.skillIds()));
                /*
                 * Only this join multiplies rows: a job with three of the
                 * requested skills would otherwise come back three times and
                 * eat three slots of the page. Left off the other branches
                 * because DISTINCT over the whole select list is not free.
                 */
                query.distinct(true);
            }

            // workMode / jobType / experienceLevel - free-text columns, so
            // matched case-insensitively against the values the client sent
            addInIgnoringCase(predicates, cb, root, "workMode", filter.workModes());
            addInIgnoringCase(predicates, cb, root, "jobType", filter.jobTypes());
            addInIgnoringCase(predicates, cb, root, "experienceLevel", filter.experienceLevels());

            /*
             * Salary is a range per job, so the two bounds are compared against
             * opposite ends of it: "pays at least X" asks the top of the range,
             * "costs at most Y" asks the bottom. A half-open range is judged by
             * the end it does have; a job with neither bound advertises no
             * salary and cannot satisfy either filter.
             */
            if (filter.salaryMin() != null) {
                Expression<BigDecimal> ceiling = cb.coalesce(root.get("salaryMax"), root.get("salaryMin"));
                predicates.add(cb.greaterThanOrEqualTo(ceiling, filter.salaryMin()));
            }
            if (filter.salaryMax() != null) {
                Expression<BigDecimal> floor = cb.coalesce(root.get("salaryMin"), root.get("salaryMax"));
                predicates.add(cb.lessThanOrEqualTo(floor, filter.salaryMax()));
            }

            // postedAfter
            if (filter.postedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), filter.postedAfter()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Carries the listing's ordering, and nothing else: the predicate it
     * contributes is always true.
     *
     * <p>Ordering lives here rather than on the {@code Pageable} because a job
     * that names no salary has to sort last, and there is no other way to say
     * so. Spring Data's {@code Sort.Order#nullsLast} is dropped on the floor
     * for the criteria queries a specification builds, and PostgreSQL's own
     * default puts those nulls first — so "highest salary" would open on the
     * postings with no salary at all. Sorting a coalesced expression instead
     * says it in a way every database honours.
     *
     * <p>Every ordering ends on the primary key. Two jobs published in the same
     * second are otherwise ordered by whatever the database feels like, and
     * page 2 can repeat a row page 1 already showed or skip one entirely.
     *
     * <p>Safe on the count query Spring Data derives from the same
     * specification: it clears the orders before counting.
     */
    public static Specification<JobPost> orderedBy(Sort sort) {
        return (root, query, cb) -> {
            List<Order> orders = new ArrayList<>();

            for (Sort.Order requested : sort) {
                Expression<?> expression = orderExpression(root, cb, requested);
                orders.add(requested.isAscending() ? cb.asc(expression) : cb.desc(expression));
            }

            if (sort.getOrderFor("id") == null) {
                orders.add(cb.desc(root.get("id")));
            }

            query.orderBy(orders);

            return cb.conjunction();
        };
    }

    private static Expression<?> orderExpression(Root<JobPost> root, CriteriaBuilder cb, Sort.Order requested) {
        if (!SALARY_PROPERTIES.contains(requested.getProperty())) {
            return root.get(requested.getProperty());
        }

        return cb.coalesce(
                root.get(requested.getProperty()),
                requested.isAscending() ? ABOVE_ANY_SALARY : BELOW_ANY_SALARY
        );
    }

    private static void addInIgnoringCase(
            List<Predicate> predicates,
            CriteriaBuilder cb,
            Root<JobPost> root,
            String attribute,
            List<String> values
    ) {
        if (!hasValues(values)) {
            return;
        }

        List<String> lowered = values.stream()
                .filter(JobPostSpecification::hasText)
                .map(value -> value.trim().toLowerCase())
                .distinct()
                .toList();

        if (lowered.isEmpty()) {
            return;
        }

        Path<String> path = root.get(attribute);
        predicates.add(cb.lower(path).in(lowered));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasValues(List<?> values) {
        return values != null && !values.isEmpty();
    }
}
