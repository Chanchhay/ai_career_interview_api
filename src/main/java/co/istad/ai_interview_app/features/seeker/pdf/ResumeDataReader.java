package co.istad.ai_interview_app.features.seeker.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns the editor's free-form {@code resumeData} JSON into a
 * {@link ResumeDocumentModel}.
 *
 * <p>Mirrors {@code normalizeResumeData} in the frontend, including its
 * tolerance for the older flat shape where experience and education were single
 * blocks of text. A resume written by any version of the builder must still
 * render — a PDF that throws because a field moved two releases ago is worse
 * than one that renders a section short.
 */
@Component
public class ResumeDataReader {

    private static final String DEFAULT_TEMPLATE_ID = "classic";
    private static final String DEFAULT_ACCENT = "#059669";

    public ResumeDocumentModel read(Map<String, Object> resumeData) {
        Map<String, Object> source = resumeData == null ? Map.of() : resumeData;

        return new ResumeDocumentModel(
                textOr(source.get("templateId"), DEFAULT_TEMPLATE_ID),
                validAccent(text(source.get("accent"))),
                text(source.get("fullName")),
                text(source.get("professionalTitle")),
                text(source.get("email")),
                text(source.get("phone")),
                text(source.get("location")),
                text(source.get("summary")),
                readSkills(source.get("skills")),
                readExperience(source.get("experience")),
                readEducation(source.get("education")),
                readProjects(source.get("projects")),
                readLinks(source.get("links"))
        );
    }

    /**
     * The accent goes straight into a stylesheet, so anything that is not a
     * plain hex colour is discarded rather than escaped — this is the one field
     * whose value becomes CSS rather than text.
     */
    private String validAccent(String value) {
        return value.matches("#[0-9a-fA-F]{3,8}") ? value : DEFAULT_ACCENT;
    }

    private List<String> readSkills(Object raw) {
        List<String> skills = new ArrayList<>();

        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String skill = text(item);
                if (!skill.isBlank()) skills.add(skill);
            }
        } else {
            // Legacy: one comma-separated string.
            for (String part : text(raw).split(",")) {
                if (!part.isBlank()) skills.add(part.trim());
            }
        }

        return skills;
    }

    private List<ResumeDocumentModel.Experience> readExperience(Object raw) {
        List<ResumeDocumentModel.Experience> entries = new ArrayList<>();

        for (Map<String, Object> item : asMaps(raw)) {
            entries.add(new ResumeDocumentModel.Experience(
                    text(item.get("role")),
                    text(item.get("company")),
                    text(item.get("location")),
                    text(item.get("start")),
                    text(item.get("end")),
                    Boolean.TRUE.equals(item.get("current")),
                    text(item.get("description"))
            ));
        }

        if (entries.isEmpty() && !text(raw).isBlank()) {
            entries.add(new ResumeDocumentModel.Experience(
                    "", "", "", "", "", false, text(raw)
            ));
        }

        return entries;
    }

    private List<ResumeDocumentModel.Education> readEducation(Object raw) {
        List<ResumeDocumentModel.Education> entries = new ArrayList<>();

        for (Map<String, Object> item : asMaps(raw)) {
            entries.add(new ResumeDocumentModel.Education(
                    text(item.get("degree")),
                    text(item.get("school")),
                    text(item.get("year")),
                    text(item.get("description"))
            ));
        }

        if (entries.isEmpty() && !text(raw).isBlank()) {
            entries.add(new ResumeDocumentModel.Education("", "", "", text(raw)));
        }

        return entries;
    }

    private List<ResumeDocumentModel.Project> readProjects(Object raw) {
        List<ResumeDocumentModel.Project> entries = new ArrayList<>();

        for (Map<String, Object> item : asMaps(raw)) {
            entries.add(new ResumeDocumentModel.Project(
                    text(item.get("name")),
                    text(item.get("url")),
                    text(item.get("description"))
            ));
        }

        return entries;
    }

    private List<ResumeDocumentModel.Link> readLinks(Object raw) {
        List<ResumeDocumentModel.Link> entries = new ArrayList<>();

        for (Map<String, Object> item : asMaps(raw)) {
            String url = text(item.get("url"));
            if (url.isBlank()) continue;

            String label = text(item.get("label"));
            entries.add(new ResumeDocumentModel.Link(label.isBlank() ? url : label, url));
        }

        return entries;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMaps(Object raw) {
        List<Map<String, Object>> maps = new ArrayList<>();

        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    maps.add((Map<String, Object>) map);
                }
            }
        }

        return maps;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String textOr(Object value, String fallback) {
        String resolved = text(value);
        return resolved.isBlank() ? fallback : resolved;
    }
}
