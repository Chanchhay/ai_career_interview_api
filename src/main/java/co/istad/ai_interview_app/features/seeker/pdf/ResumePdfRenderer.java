package co.istad.ai_interview_app.features.seeker.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Renders a stored resume to PDF bytes.
 *
 * <p>Deterministic: the same {@code resumeData} produces the same document
 * every time, because nothing here reads the clock, the network, or the caller.
 * That is what makes a regenerated resume comparable to the one that was
 * attached to an application.
 *
 * <p><strong>Fonts.</strong> The PDF base-14 faces cover Latin only, which
 * would render a Khmer name as a row of blanks — not an edge case on this
 * platform. Noto Sans Khmer is embedded from resources and named as a fallback
 * family in the stylesheet, so Latin text keeps the base-14 metrics and Khmer
 * glyphs resolve against the embedded face. Thai, Arabic, and CJK still have no
 * coverage; each needs its own font registered the same way.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumePdfRenderer {

    /** Must match the fallback family named in {@link ResumeHtmlBuilder}. */
    public static final String KHMER_FONT_FAMILY = "Noto Sans Khmer";

    private static final String KHMER_FONT_RESOURCE = "/fonts/NotoSansKhmer-Regular.ttf";

    private final ResumeDataReader reader;
    private final ResumeHtmlBuilder htmlBuilder;

    public byte[] render(Map<String, Object> resumeData) {
        ResumeDocumentModel model = reader.read(resumeData);
        String html = htmlBuilder.build(model);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            /*
             * No base URI on purpose. Without one the renderer cannot resolve a
             * relative href, which means resume content can never make the
             * server fetch a URL of the candidate's choosing.
             */
            builder.withHtmlContent(html, null);
            registerKhmerFont(builder);
            builder.toStream(output);
            builder.run();

            return output.toByteArray();
        } catch (Exception exception) {
            log.error("Resume PDF render failed", exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate the resume PDF"
            );
        }
    }

    /**
     * Supplies the embedded Khmer face to the renderer.
     *
     * <p>A missing or unreadable font must not stop a resume from being
     * generated: Latin resumes are the common case and they render fine without
     * it. The failure is logged loudly instead, because the symptom otherwise —
     * blank glyphs in one candidate's PDF — is nearly impossible to trace back.
     */
    private void registerKhmerFont(PdfRendererBuilder builder) {
        try {
            byte[] font;

            try (InputStream stream = getClass().getResourceAsStream(KHMER_FONT_RESOURCE)) {
                if (stream == null) {
                    log.warn("{} is missing; Khmer text will render blank", KHMER_FONT_RESOURCE);
                    return;
                }

                font = stream.readAllBytes();
            }

            // Re-read from the bytes on each use: the supplier can be called
            // more than once per document, and a consumed stream would yield an
            // empty font the second time.
            builder.useFont(() -> new ByteArrayInputStream(font), KHMER_FONT_FAMILY);
        } catch (Exception exception) {
            log.error("Could not load the Khmer font; Khmer text will render blank", exception);
        }
    }
}
