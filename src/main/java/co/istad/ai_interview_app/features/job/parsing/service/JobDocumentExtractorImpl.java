package co.istad.ai_interview_app.features.job.parsing.service;

import co.istad.ai_interview_app.features.job.parsing.dto.ExtractedJobDocument;
import co.istad.ai_interview_app.shared.exception.GeminiGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

@Component
@RequiredArgsConstructor
public class JobDocumentExtractorImpl implements JobDocumentExtractor {

    private final ChatClient geminiChatClient;

    @Override
    public ExtractedJobDocument extract(
            String documentText,
            List<String> categoryNames
    ) {
        ExtractedJobDocument result = geminiChatClient
                .prompt()
                .system("""
                        You extract structured job posting data from the raw
                        text of a job description document.

                        Rules:
                        - Only report what the document states. Never invent a
                          value. Leave a field null when the document does not
                          state it — an empty field is far better than a guess.
                        - Do not summarise away detail. The description and the
                          sections should keep the wording of the document,
                          reformatted as clean markdown.
                        - description: the role overview and mission only. Put
                          requirements, responsibilities, benefits and
                          qualifications in sections instead, not here.
                        - sections: one entry per themed block you find, using
                          these types only:
                          DESCRIPTION, REQUIREMENT_RESPONSIBILITY, BENEFIT,
                          QUALIFICATION, NICE_TO_HAVE, ABOUT_ROLE.
                          Use each type at most once, merging related blocks.
                          Write contentMarkdown as markdown bullet lists.
                          Give each section a short human title.
                        - jobType: one of FULL_TIME, PART_TIME, CONTRACT,
                          INTERNSHIP, TEMPORARY, or null.
                        - workMode: one of ONSITE, HYBRID, REMOTE, or null.
                        - experienceLevel: one of ENTRY, JUNIOR, MID, SENIOR,
                          LEAD, or null. Map years of experience sensibly, but
                          only when the document gives them.
                        - salaryMin and salaryMax: plain yearly numbers with no
                          currency symbol or separators. If the document gives a
                          single figure, use it for both. If it gives a monthly
                          figure, keep it monthly rather than converting. Null
                          when no salary is stated.
                        - categoryName: choose the closest match from the
                          provided list of existing categories, copied exactly.
                          Null when none of them fit.
                        - skills: concrete technologies, tools and named
                          competencies the role calls for, one per entry.
                          Do not include soft-skill phrases such as
                          "good communication" or "team player".
                          Give each a name exactly as the industry writes it
                          ("PostgreSQL", not "postgres"), and a skillType of
                          LANGUAGE, FRAMEWORK, LIBRARY, TOOL, PLATFORM,
                          DATABASE, METHODOLOGY or DOMAIN. Use null for
                          skillType only when none of those fit.
                        - Reply in English even when the document is not.
                        """)
                .user(user -> user
                        .text("""
                                Existing job categories:
                                {categoryNames}

                                Job description document:
                                {documentText}
                                """)
                        .param(
                                "categoryNames",
                                categoryNames.isEmpty()
                                        ? "(none defined — return null)"
                                        : String.join(", ", categoryNames)
                        )
                        .param("documentText", documentText)
                )
                .call()
                .entity(
                        ExtractedJobDocument.class,
                        specification -> specification
                                .useProviderStructuredOutput()
                                .validateSchema()
                );

        validate(result);

        return result;
    }

    /**
     * A document with no title and no body text means the extraction found
     * nothing usable — most often because the upload was not a job description
     * at all. Prefilling the form with that is worse than failing.
     */
    private void validate(ExtractedJobDocument result) {
        if (result == null) {
            throw new GeminiGenerationException(
                    "Gemini returned no job data for the uploaded document"
            );
        }

        boolean hasSectionContent = result.sections() != null
                && result.sections().stream().anyMatch(section ->
                section != null && hasText(section.contentMarkdown()));

        if (!hasText(result.title())
                && !hasText(result.description())
                && !hasSectionContent) {
            throw new GeminiGenerationException(
                    "Gemini found no job posting details in the uploaded document"
            );
        }
    }
}
