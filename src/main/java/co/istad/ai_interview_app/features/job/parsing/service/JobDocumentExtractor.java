package co.istad.ai_interview_app.features.job.parsing.service;

import co.istad.ai_interview_app.features.job.parsing.dto.ExtractedJobDocument;

import java.util.List;

public interface JobDocumentExtractor {

    /**
     * Turns the plain text of a job description into the fields a job post is
     * made of.
     *
     * @param documentText   text lifted out of the uploaded PDF
     * @param categoryNames  the categories that exist, so the model picks an
     *                       existing one instead of inventing a label
     */
    ExtractedJobDocument extract(String documentText, List<String> categoryNames);
}
