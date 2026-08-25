package co.istad.ai_interview_app.features.seeker.pdf;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the XHTML that becomes the PDF.
 *
 * <p>Hand-written rather than templated through Thymeleaf for one reason:
 * openhtmltopdf requires well-formed XHTML, and a templating engine that emits
 * an unclosed tag fails at render time with a parser error that says nothing
 * about which resume broke. Building the string here keeps well-formedness a
 * property of this file alone.
 *
 * <p>Four layouts, matching the ids the frontend editor already writes into
 * {@code resumeData.templateId} — classic, minimal, modern, elegant — so the
 * PDF is recognisably the document the candidate previewed. It is not a
 * pixel-for-pixel copy of the React components: those use a browser layout
 * engine and this uses a CSS 2.1 renderer.
 */
@Component
public class ResumeHtmlBuilder {

    public String build(ResumeDocumentModel resume) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                  <title>%s</title>
                  <style>%s</style>
                </head>
                <body>%s</body>
                </html>
                """.formatted(
                escape(documentTitle(resume)),
                css(resume),
                body(resume)
        );
    }

    private String documentTitle(ResumeDocumentModel resume) {
        return resume.fullName().isBlank() ? "Resume" : resume.fullName() + " — Resume";
    }

    /* --------------------------------------------------------------- css --- */

    private String css(ResumeDocumentModel resume) {
        String accent = resume.accent();

        return """
                @page { size: A4; margin: 16mm 14mm; }
                body {
                  /* Latin keeps the base-14 metrics; Khmer resolves against the
                     embedded face registered by ResumePdfRenderer. */
                  font-family: 'Helvetica', 'Noto Sans Khmer', 'Arial', sans-serif;
                  font-size: 10.5pt;
                  line-height: 1.45;
                  color: #1f2933;
                  margin: 0;
                }
                h1 { font-size: 21pt; margin: 0; color: #0f172a; }
                h2 {
                  font-size: 10pt;
                  letter-spacing: 0.09em;
                  text-transform: uppercase;
                  color: %1$s;
                  margin: 15px 0 6px;
                  padding-bottom: 3px;
                  border-bottom: 1px solid #e2e8f0;
                }
                h3 { font-size: 11pt; margin: 0; color: #0f172a; }
                p { margin: 0 0 4px; }
                ul { margin: 0; padding-left: 15px; }
                .title { font-size: 12pt; color: %1$s; margin-top: 3px; }
                .contact { font-size: 9.5pt; color: #52606d; margin-top: 5px; }
                /* Entries must not be split across a page break mid-heading. */
                .entry { margin-bottom: 9px; page-break-inside: avoid; }
                .entry-head { margin-bottom: 2px; }
                .meta { font-size: 9.5pt; color: #52606d; }
                .desc { font-size: 10pt; color: #3e4c59; white-space: pre-wrap; }
                .skill {
                  display: inline-block;
                  border: 1px solid #cbd2d9;
                  border-radius: 9px;
                  padding: 1px 7px;
                  margin: 0 4px 4px 0;
                  font-size: 9pt;
                }
                a { color: %1$s; text-decoration: none; }

                /* classic — accent rule under a left-aligned header */
                .classic .header { border-bottom: 3px solid %1$s; padding-bottom: 8px; }

                /* minimal — no accent furniture at all, safest for parsers */
                .minimal h2 { color: #0f172a; border-bottom: 1px solid #cbd2d9; }
                .minimal .title { color: #52606d; }

                /* modern — filled header band */
                .modern .header {
                  background-color: %1$s;
                  color: #ffffff;
                  padding: 14px 16px;
                  margin: -4px -6px 10px;
                }
                .modern .header h1, .modern .header .title { color: #ffffff; }
                .modern .header .contact { color: #f1f5f9; }

                /* elegant — centred header, serif face */
                .elegant { font-family: 'Times New Roman', 'Noto Sans Khmer', 'Georgia', serif; }
                .elegant .header { text-align: center; padding-bottom: 10px; }
                .elegant h2 { text-align: center; border-bottom: none; border-top: 1px solid #cbd2d9; padding-top: 6px; }
                """.formatted(accent);
    }

    /* -------------------------------------------------------------- body --- */

    private String body(ResumeDocumentModel resume) {
        StringBuilder html = new StringBuilder();

        // An unknown id falls back to classic rather than producing an unstyled
        // page: templateId comes from stored JSON and may name a layout that no
        // longer exists.
        String layout = switch (resume.templateId()) {
            case "minimal", "modern", "elegant" -> resume.templateId();
            default -> "classic";
        };

        html.append("<div class=\"").append(layout).append("\">");
        html.append(header(resume));

        if (!resume.summary().isBlank()) {
            html.append(section("Summary", "<p class=\"desc\">" + escape(resume.summary()) + "</p>"));
        }

        if (!resume.experience().isEmpty()) {
            html.append(section("Experience", experience(resume)));
        }

        if (!resume.education().isEmpty()) {
            html.append(section("Education", education(resume)));
        }

        if (!resume.projects().isEmpty()) {
            html.append(section("Projects", projects(resume)));
        }

        if (!resume.skills().isEmpty()) {
            html.append(section("Skills", skills(resume)));
        }

        if (!resume.links().isEmpty()) {
            html.append(section("Links", links(resume)));
        }

        html.append("</div>");

        return html.toString();
    }

    private String header(ResumeDocumentModel resume) {
        StringBuilder html = new StringBuilder("<div class=\"header\">");

        html.append("<h1>")
                .append(escape(resume.fullName().isBlank() ? "Unnamed" : resume.fullName()))
                .append("</h1>");

        if (!resume.professionalTitle().isBlank()) {
            html.append("<div class=\"title\">").append(escape(resume.professionalTitle())).append("</div>");
        }

        if (resume.hasContact()) {
            html.append("<div class=\"contact\">")
                    .append(escape(joinNonBlank(" · ", resume.email(), resume.phone(), resume.location())))
                    .append("</div>");
        }

        return html.append("</div>").toString();
    }

    private String experience(ResumeDocumentModel resume) {
        StringBuilder html = new StringBuilder();

        for (ResumeDocumentModel.Experience entry : resume.experience()) {
            html.append("<div class=\"entry\"><div class=\"entry-head\"><h3>")
                    .append(escape(joinNonBlank(" — ", entry.role(), entry.company())))
                    .append("</h3>");

            String meta = joinNonBlank(" · ", entry.period(), entry.location());
            if (!meta.isBlank()) {
                html.append("<div class=\"meta\">").append(escape(meta)).append("</div>");
            }

            html.append("</div>");

            if (!entry.description().isBlank()) {
                html.append("<div class=\"desc\">").append(escape(entry.description())).append("</div>");
            }

            html.append("</div>");
        }

        return html.toString();
    }

    private String education(ResumeDocumentModel resume) {
        StringBuilder html = new StringBuilder();

        for (ResumeDocumentModel.Education entry : resume.education()) {
            html.append("<div class=\"entry\"><div class=\"entry-head\"><h3>")
                    .append(escape(joinNonBlank(" — ", entry.degree(), entry.school())))
                    .append("</h3>");

            if (!entry.year().isBlank()) {
                html.append("<div class=\"meta\">").append(escape(entry.year())).append("</div>");
            }

            html.append("</div>");

            if (!entry.description().isBlank()) {
                html.append("<div class=\"desc\">").append(escape(entry.description())).append("</div>");
            }

            html.append("</div>");
        }

        return html.toString();
    }

    private String projects(ResumeDocumentModel resume) {
        StringBuilder html = new StringBuilder();

        for (ResumeDocumentModel.Project entry : resume.projects()) {
            html.append("<div class=\"entry\"><h3>").append(escape(entry.name())).append("</h3>");

            if (!entry.url().isBlank()) {
                html.append("<div class=\"meta\">").append(escape(entry.url())).append("</div>");
            }

            if (!entry.description().isBlank()) {
                html.append("<div class=\"desc\">").append(escape(entry.description())).append("</div>");
            }

            html.append("</div>");
        }

        return html.toString();
    }

    private String skills(ResumeDocumentModel resume) {
        StringBuilder html = new StringBuilder("<div>");

        for (String skill : resume.skills()) {
            html.append("<span class=\"skill\">").append(escape(skill)).append("</span>");
        }

        return html.append("</div>").toString();
    }

    private String links(ResumeDocumentModel resume) {
        StringBuilder html = new StringBuilder("<ul>");

        for (ResumeDocumentModel.Link link : resume.links()) {
            html.append("<li><a href=\"")
                    .append(escape(link.url()))
                    .append("\">")
                    .append(escape(link.label()))
                    .append("</a></li>");
        }

        return html.append("</ul>").toString();
    }

    private String section(String heading, String content) {
        return "<h2>" + escape(heading) + "</h2>" + content;
    }

    private String joinNonBlank(String separator, String... parts) {
        return String.join(
                separator,
                List.of(parts).stream().filter(part -> part != null && !part.isBlank()).toList()
        );
    }

    /**
     * Escapes for XML, not just HTML.
     *
     * <p>openhtmltopdf parses its input as XML, so a stray {@code &} in a job
     * title fails the whole render rather than displaying oddly. Everything the
     * user typed goes through here.
     */
    private String escape(String value) {
        if (value == null) return "";

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
