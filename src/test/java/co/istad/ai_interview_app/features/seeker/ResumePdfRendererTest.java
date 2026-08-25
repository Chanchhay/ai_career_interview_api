package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.seeker.pdf.ResumeDataReader;
import co.istad.ai_interview_app.features.seeker.pdf.ResumeHtmlBuilder;
import co.istad.ai_interview_app.features.seeker.pdf.ResumePdfRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the renderer produces a real PDF from the JSON the editor writes.
 *
 * <p>Also the standing check that openhtmltopdf and PDFBox 3 agree at runtime:
 * the widely used {@code com.openhtmltopdf} coordinates target PDFBox 2, and
 * swapping to them would compile fine and fail only here.
 */
class ResumePdfRendererTest {

    private final ResumePdfRenderer renderer = new ResumePdfRenderer(
            new ResumeDataReader(),
            new ResumeHtmlBuilder()
    );

    private String textOf(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void rendersTheEditorsShapeIntoAReadablePdf() throws Exception {
        Map<String, Object> resumeData = Map.of(
                "templateId", "classic",
                "accent", "#059669",
                "fullName", "Sokha Chan",
                "professionalTitle", "Backend Developer",
                "email", "sokha@example.com",
                "location", "Phnom Penh",
                "summary", "Builds Spring Boot services.",
                "skills", List.of("Java", "PostgreSQL"),
                "experience", List.of(Map.of(
                        "role", "Backend Developer",
                        "company", "Example Co",
                        "start", "2025-01",
                        "current", true,
                        "description", "Built REST APIs."
                ))
        );

        byte[] pdf = renderer.render(resumeData);

        assertThat(pdf).isNotEmpty();
        // %PDF- magic bytes: proves this is a document, not an error page.
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        String text = textOf(pdf);
        assertThat(text).contains("Sokha Chan");
        assertThat(text).contains("Backend Developer");
        assertThat(text).contains("Example Co");
        assertThat(text).contains("Java");
        // "current": true must render as Present rather than an empty end date.
        assertThat(text).contains("Present");
    }

    @Test
    void rendersEveryTemplateIdAndFallsBackForAnUnknownOne() {
        for (String templateId : List.of("classic", "minimal", "modern", "elegant", "no-such-template")) {
            byte[] pdf = renderer.render(Map.of(
                    "templateId", templateId,
                    "fullName", "Sokha Chan"
            ));

            assertThat(pdf).as("template %s", templateId).isNotEmpty();
        }
    }

    /**
     * The renderer parses XML, so an unescaped ampersand or angle bracket in
     * user text would fail the whole document rather than look wrong.
     */
    @Test
    void survivesCharactersThatWouldBreakTheXmlParser() throws Exception {
        byte[] pdf = renderer.render(Map.of(
                "fullName", "A & B <script>alert('x')</script>",
                "summary", "Uses \"quotes\" & <angles>"
        ));

        String text = textOf(pdf);
        assertThat(text).contains("A & B");
        assertThat(text).contains("Uses \"quotes\" & <angles>");
    }

    /** An empty resume is a legitimate draft; it must render, not throw. */
    @Test
    void rendersAnEmptyResume() {
        assertThat(renderer.render(Map.of())).isNotEmpty();
        assertThat(renderer.render(null)).isNotEmpty();
    }

    /**
     * The accent becomes CSS rather than escaped text, so anything that is not a
     * hex colour has to be rejected before it reaches the stylesheet.
     */
    @Test
    void ignoresAnAccentThatIsNotAHexColour() throws Exception {
        byte[] pdf = renderer.render(Map.of(
                "accent", "red; } body { display: none; } h1 {",
                "fullName", "Sokha Chan"
        ));

        assertThat(textOf(pdf)).contains("Sokha Chan");
    }

    /**
     * Khmer names and content are the normal case for this platform, and the
     * PDF base-14 fonts do not cover the script — without the embedded face
     * these characters render as blanks with no error anywhere.
     *
     * <p>Asserted on the embedded font rather than only on extracted text.
     * Khmer coeng clusters are shaped into single ligature glyphs, and PDFBox's
     * extractor cannot always map those back to their source codepoints — so a
     * text assertion would fail on a document that displays perfectly. Font
     * embedding is what actually decides whether the glyphs appear.
     */
    @Test
    void embedsTheKhmerFontAndRendersKhmerText() throws Exception {
        byte[] pdf = renderer.render(Map.of(
                "fullName", "សុខា ចាន់",
                "professionalTitle", "អ្នកអភិវឌ្ឍន៍កម្មវិធី",
                "summary", "បង្កើតកម្មវិធីតាមអ៊ីនធឺណិត។"
        ));

        assertThat(pdf).isNotEmpty();
        assertThat(embeddedFontNames(pdf))
                .as("embedded fonts")
                .anyMatch(name -> name.contains("Khmer"));

        // Unshaped Khmer does survive extraction, so it is still worth asserting.
        assertThat(textOf(pdf)).contains("សុខា");
    }

    /** Latin and Khmer in one document, which a bilingual resume will mix. */
    @Test
    void rendersLatinAndKhmerTogether() throws Exception {
        byte[] pdf = renderer.render(Map.of(
                "fullName", "Sokha Chan / សុខា ចាន់",
                "skills", List.of("Java", "PostgreSQL")
        ));

        String text = textOf(pdf);
        assertThat(text).contains("Sokha Chan");
        assertThat(text).contains("សុខា");
        assertThat(text).contains("Java");
    }

    /** Font names of every face the document actually carries. */
    private List<String> embeddedFontNames(byte[] pdf) throws Exception {
        List<String> names = new java.util.ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();

                for (COSName fontName : resources.getFontNames()) {
                    PDFont font = resources.getFont(fontName);
                    if (font != null) names.add(font.getName());
                }
            }
        }

        return names;
    }
}
