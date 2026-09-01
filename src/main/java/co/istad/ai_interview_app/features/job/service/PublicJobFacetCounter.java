package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.job.dto.PublicJobFacetsResponse.PublicJobFacetOption;
import co.istad.ai_interview_app.features.job.dto.PublicJobFacetsResponse.PublicJobFacetValue;
import co.istad.ai_interview_app.features.job.dto.PublicJobFacetsResponse.SalaryBounds;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Counts how many jobs sit behind each option a filter could still offer.
 *
 * <p>Every count is over <em>distinct</em> jobs. A specification that joins the
 * skills table returns a job once per skill it matched, and a facet that
 * counted rows would claim more jobs than exist.
 */
@Component
@RequiredArgsConstructor
public class PublicJobFacetCounter {

    /** Enough skills to fill a sidebar; the long tail is not worth the scroll. */
    private static final int SKILL_LIMIT = 25;

    private final EntityManager entityManager;

    /** Counts per distinct value of a free-text column, case folded so that
     *  "full_time" and "FULL_TIME" are one option rather than two. */
    public List<PublicJobFacetValue> countByColumn(Specification<JobPost> spec, String attribute) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<JobPost> root = query.from(JobPost.class);

        Predicate predicate = spec.toPredicate(root, query, cb);
        Expression<String> value = cb.upper(root.get(attribute));
        Expression<Long> count = cb.countDistinct(root.get("id"));

        query.select(cb.tuple(value, count))
                .groupBy(value)
                .orderBy(cb.desc(count), cb.asc(value));
        if (predicate != null) {
            query.where(predicate);
        }

        return entityManager.createQuery(query).getResultList().stream()
                .filter(tuple -> hasText(tuple.get(0, String.class)))
                .map(tuple -> new PublicJobFacetValue(tuple.get(0, String.class), tuple.get(1, Long.class)))
                .toList();
    }

    /** Counts per job category. */
    public List<PublicJobFacetOption> countByCategory(Specification<JobPost> spec) {
        return countByJoinedRow(spec, root -> root.join("category", JoinType.INNER), 0);
    }

    /** Counts per skill a job asks for, most asked first. */
    public List<PublicJobFacetOption> countBySkill(Specification<JobPost> spec) {
        return countByJoinedRow(
                spec,
                root -> root.join("skills", JoinType.INNER).join("skill", JoinType.INNER),
                SKILL_LIMIT
        );
    }

    /** The salary span the matching jobs cover, or null when none names one. */
    public SalaryBounds salaryBounds(Specification<JobPost> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<JobPost> root = query.from(JobPost.class);

        Predicate predicate = spec.toPredicate(root, query, cb);
        // A job may advertise one end of its range only; the bound it does have
        // stands in for the one it does not.
        Expression<BigDecimal> floor = cb.coalesce(root.get("salaryMin"), root.get("salaryMax"));
        Expression<BigDecimal> ceiling = cb.coalesce(root.get("salaryMax"), root.get("salaryMin"));

        query.select(cb.tuple(cb.min(floor), cb.max(ceiling)));
        if (predicate != null) {
            query.where(predicate);
        }

        Tuple result = entityManager.createQuery(query).getSingleResult();
        BigDecimal min = result.get(0, BigDecimal.class);
        BigDecimal max = result.get(1, BigDecimal.class);

        return min == null || max == null ? null : new SalaryBounds(min, max);
    }

    /** How many distinct jobs the specification matches. */
    public long count(Specification<JobPost> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<JobPost> root = query.from(JobPost.class);

        Predicate predicate = spec.toPredicate(root, query, cb);
        query.select(cb.countDistinct(root.get("id")));
        if (predicate != null) {
            query.where(predicate);
        }

        return entityManager.createQuery(query).getSingleResult();
    }

    private List<PublicJobFacetOption> countByJoinedRow(
            Specification<JobPost> spec,
            java.util.function.Function<Root<JobPost>, Join<?, ?>> joiner,
            int limit
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<JobPost> root = query.from(JobPost.class);

        Predicate predicate = spec.toPredicate(root, query, cb);
        Join<?, ?> joined = joiner.apply(root);
        Expression<Long> count = cb.countDistinct(root.get("id"));

        query.select(cb.tuple(joined.get("id"), joined.get("name"), count))
                .groupBy(joined.get("id"), joined.get("name"))
                .orderBy(cb.desc(count), cb.asc(joined.get("name")));
        if (predicate != null) {
            query.where(predicate);
        }

        var typedQuery = entityManager.createQuery(query);
        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }

        return typedQuery.getResultList().stream()
                .map(tuple -> new PublicJobFacetOption(
                        tuple.get(0, Long.class),
                        tuple.get(1, String.class),
                        tuple.get(2, Long.class)
                ))
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
