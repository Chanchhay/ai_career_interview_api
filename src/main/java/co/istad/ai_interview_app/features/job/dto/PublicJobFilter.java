package co.istad.ai_interview_app.features.job.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The caller-supplied half of a public job search: everything a visitor can
 * narrow the listing by. The non-negotiable half — published, from a verified
 * and active company, not expired — is not in here; the service supplies it so
 * that no request can widen it.
 *
 * <p>A {@code null} or empty member means "no constraint". The multi-valued
 * members match any of their values (OR within a filter, AND between filters),
 * which is what the checkbox groups on the job board expect.
 */
public record PublicJobFilter(
        String keyword,
        String location,
        List<Long> categoryIds,
        List<Long> skillIds,
        List<String> workModes,
        List<String> jobTypes,
        List<String> experienceLevels,
        // Keeps only jobs that can pay at least this much, read against the
        // top of the job's range. A job that advertises no salary at all is
        // dropped once this is set - there is nothing to compare it against.
        BigDecimal salaryMin,
        // Keeps only jobs whose range starts at or below this.
        BigDecimal salaryMax,
        // Keeps only jobs published since this moment.
        Instant postedAfter
) {

    public PublicJobFilter withPostedAfter(Instant moment) {
        return new PublicJobFilter(keyword, location, categoryIds, skillIds, workModes, jobTypes,
                experienceLevels, salaryMin, salaryMax, moment);
    }

    /** The same filter with one dimension lifted, for counting that dimension. */
    public PublicJobFilter withoutCategories() {
        return new PublicJobFilter(keyword, location, List.of(), skillIds, workModes, jobTypes,
                experienceLevels, salaryMin, salaryMax, postedAfter);
    }

    public PublicJobFilter withoutSkills() {
        return new PublicJobFilter(keyword, location, categoryIds, List.of(), workModes, jobTypes,
                experienceLevels, salaryMin, salaryMax, postedAfter);
    }

    public PublicJobFilter withoutWorkModes() {
        return new PublicJobFilter(keyword, location, categoryIds, skillIds, List.of(), jobTypes,
                experienceLevels, salaryMin, salaryMax, postedAfter);
    }

    public PublicJobFilter withoutJobTypes() {
        return new PublicJobFilter(keyword, location, categoryIds, skillIds, workModes, List.of(),
                experienceLevels, salaryMin, salaryMax, postedAfter);
    }

    public PublicJobFilter withoutExperienceLevels() {
        return new PublicJobFilter(keyword, location, categoryIds, skillIds, workModes, jobTypes,
                List.of(), salaryMin, salaryMax, postedAfter);
    }

    public PublicJobFilter withoutSalary() {
        return new PublicJobFilter(keyword, location, categoryIds, skillIds, workModes, jobTypes,
                experienceLevels, null, null, postedAfter);
    }
}
