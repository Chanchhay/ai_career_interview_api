package co.istad.ai_interview_app.features.job.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the current search could still be narrowed by, and by how much.
 *
 * <p>Each group is counted with every filter applied <em>except its own</em>.
 * Counting a group against itself would drive its unchosen options to zero the
 * moment one is ticked, and a sidebar that empties itself on the first click
 * cannot be used to widen a search.
 *
 * <p>Only values the matching jobs actually carry appear, so an option is never
 * offered that leads nowhere.
 */
public record PublicJobFacetsResponse(
        List<PublicJobFacetValue> jobTypes,
        List<PublicJobFacetValue> workModes,
        List<PublicJobFacetValue> experienceLevels,
        List<PublicJobFacetOption> categories,
        List<PublicJobFacetOption> skills,
        List<PublicJobFacetValue> postedWithin,
        // Null when no matching job names a salary at all.
        SalaryBounds salaryRange,
        long totalJobs
) {

    /** A free-text column's value - job type, work mode, experience level. */
    public record PublicJobFacetValue(String value, long count) {
    }

    /** A referenced row - a category or a skill - named for display. */
    public record PublicJobFacetOption(Long id, String name, long count) {
    }

    /** The salary span the matching jobs actually cover, for the slider bounds. */
    public record SalaryBounds(BigDecimal min, BigDecimal max) {
    }
}
