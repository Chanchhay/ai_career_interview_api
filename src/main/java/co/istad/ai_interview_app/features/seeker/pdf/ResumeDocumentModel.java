package co.istad.ai_interview_app.features.seeker.pdf;

import java.util.List;

/**
 * The resume as the renderer needs it: every field a String or a list, nothing
 * null, nothing to interpret.
 *
 * <p>{@code resumeData} is free-form JSON owned by the frontend editor, so this
 * is where that looseness stops. {@link ResumeDataReader} does the coercion once
 * and the templates below it can assume clean input.
 */
public record ResumeDocumentModel(
        String templateId,
        String accent,
        String fullName,
        String professionalTitle,
        String email,
        String phone,
        String location,
        String summary,
        List<String> skills,
        List<Experience> experience,
        List<Education> education,
        List<Project> projects,
        List<Link> links
) {

    public record Experience(
            String role,
            String company,
            String location,
            String start,
            String end,
            boolean current,
            String description
    ) {
        /** "Jan 2024 — Present", or whichever half of it exists. */
        public String period() {
            String from = start == null ? "" : start;
            String to = current ? "Present" : (end == null ? "" : end);

            if (from.isBlank() && to.isBlank()) return "";
            if (from.isBlank()) return to;
            if (to.isBlank()) return from;

            return from + " — " + to;
        }
    }

    public record Education(String degree, String school, String year, String description) {
    }

    public record Project(String name, String url, String description) {
    }

    public record Link(String label, String url) {
    }

    public boolean hasContact() {
        return !email.isBlank() || !phone.isBlank() || !location.isBlank();
    }
}
