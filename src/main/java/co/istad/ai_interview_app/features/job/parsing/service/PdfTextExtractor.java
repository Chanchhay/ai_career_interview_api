package co.istad.ai_interview_app.features.job.parsing.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Pulls the selectable text out of a PDF.
 *
 * <p>There is no OCR here: a PDF that is really a photograph of a page has no
 * text layer and is rejected rather than silently parsed into nothing.
 */
@Slf4j
@Component
public class PdfTextExtractor {

    /**
     * Above this many characters the tail is dropped before the text reaches
     * Gemini. Job descriptions run a few thousand characters; anything past
     * this is boilerplate, and truncating bounds both cost and latency.
     */
    private static final int MAX_CHARACTERS = 20_000;

    /**
     * A JD shorter than this is not something the model can work with — most
     * likely a scanned page whose text layer holds only a stray page number.
     */
    private static final int MIN_CHARACTERS = 200;

    public String extract(byte[] pdf) {
        String text;

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            text = stripper.getText(document);
        } catch (InvalidPasswordException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "This PDF is password protected. Remove the password and upload it again."
            );
        } catch (IOException e) {
            log.warn("Unable to read uploaded job description PDF", e);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "This file could not be read as a PDF."
            );
        }

        String normalized = normalize(text);

        if (normalized.length() < MIN_CHARACTERS) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "No selectable text was found in this PDF. Scanned or image-only documents cannot be parsed."
            );
        }

        return normalized.length() > MAX_CHARACTERS
                ? normalized.substring(0, MAX_CHARACTERS)
                : normalized;
    }

    /**
     * Collapses the runs of blank lines and trailing spaces that column layouts
     * leave behind, so the model sees prose instead of whitespace.
     */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \t]+\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
