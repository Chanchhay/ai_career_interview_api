package co.istad.ai_interview_app.features.company.service;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.shared.enums.visibility.CompanyIdentityVisibility;

/**
 * The one place that decides what a candidate is told about a company.
 *
 * <p>Every candidate-facing response goes through here rather than reading
 * {@code company.getName()} directly. A masked company that leaks its name from
 * one forgotten endpoint is not partly masked — it is not masked at all, and
 * the only defence against that is having a single rule instead of a rule
 * repeated in six mappers.
 *
 * <p>Deliberately not a Spring bean: it holds no state and needs no
 * collaborators, and being callable from a static mapper is the point.
 */
public final class CompanyIdentity {

    /**
     * Shown in place of the name. Deliberately plain and identical for every
     * masked company — anything derived from the real name (an initial, an
     * industry-and-size phrase) narrows down who it is.
     */
    public static final String MASKED_NAME = "Confidential company";

    private CompanyIdentity() {
    }

    public static boolean isMasked(Company company) {
        return company != null
                && company.getIdentityVisibility() == CompanyIdentityVisibility.MASKED;
    }

    /** The name to show a candidate. */
    public static String displayName(Company company) {
        if (company == null) return null;

        return isMasked(company) ? MASKED_NAME : company.getName();
    }

    /**
     * The company id to show a candidate, or null when masked.
     *
     * <p>Withheld rather than passed through: an id is a handle. Leaving it in
     * lets a page link to the company, and lets anyone compare two masked
     * postings and see they came from the same employer.
     */
    public static Long displayId(Company company) {
        if (company == null || isMasked(company)) return null;

        return company.getId();
    }

    /** A logo names a company as surely as its name does. */
    public static String displayLogoUrl(Company company) {
        if (company == null || isMasked(company)) return null;

        return company.getLogoUrl();
    }
}
