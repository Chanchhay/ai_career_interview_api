package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestion;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestionSet;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.exception.GeminiGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

/**
 * Asks Gemini for an interview's questions.
 *
 * <p>The shape of the interview — how many questions, of which types, worth how
 * much — is not compiled in: it arrives as an
 * {@link AiInterviewGenerationConfig} the admin console owns, and both the
 * prompt and the validation below are built from it, so the two can never
 * disagree about what was asked for.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiInterviewQuestionGeneratorImpl
        implements AiInterviewQuestionGenerator {

    private final ChatClient geminiChatClient;

    @Override
    public GeneratedQuestionSet generateQuestions(
            String jobTitle,
            String jobDescription,
            String experienceLevel,
            List<String> requiredSkills,
            AiInterviewGenerationConfig config
    ) {
        GeneratedQuestionSet result = geminiChatClient
                .prompt()
                .system(buildSystemPrompt(config))
                .user(user -> user
                        .text("""
                                Job title: {jobTitle}

                                Experience level: {experienceLevel}

                                Required skills:
                                {requiredSkills}

                                Job description:
                                {jobDescription}
                                """)
                        .param("jobTitle", jobTitle)
                        .param("experienceLevel", experienceLevel)
                        .param(
                                "requiredSkills",
                                String.join(", ", requiredSkills)
                        )
                        .param("jobDescription", jobDescription)
                )
                .call()
                .entity(
                        GeneratedQuestionSet.class,
                        specification -> specification
                                .useProviderStructuredOutput()
                                .validateSchema()
                );

        validate(result, config);

        return result;
    }

    /**
     * Assembled as a plain string rather than a template, because the admin's
     * extra instructions are free text and a stray brace in them would be read
     * as a placeholder.
     */
    private String buildSystemPrompt(AiInterviewGenerationConfig config) {
        StringBuilder prompt = new StringBuilder("""
                You are a professional technical interviewer.

                Generate interview questions that assess whether a
                candidate is suitable for the specified job.

                Requirements:
                """);

        prompt.append("- Generate exactly ")
                .append(config.questionCount())
                .append(config.questionCount() == 1 ? " question.\n" : " questions.\n");

        config.typeDistribution().forEach((type, count) -> prompt
                .append("- Include ")
                .append(count)
                .append(' ')
                .append(describe(type))
                .append(count == 1 ? " question.\n" : " questions.\n"));

        if (!config.typeDistribution().isEmpty()) {
            prompt.append("- Use only these question types: ")
                    .append(joinTypes(config.allowedTypes()))
                    .append(".\n");
        }

        prompt.append("""
                - Adapt difficulty to the experience level.
                - Avoid duplicate questions.
                - Include a private grading rubric.
                """);

        prompt.append("- Set maxScore to ")
                .append(config.maxScorePerQuestion())
                .append(" for every question.\n");

        prompt.append("- Number the questions from 1 to ")
                .append(config.questionCount())
                .append(" in the order field.\n");

        if (hasText(config.additionalInstructions())) {
            prompt.append("\nAdditional instructions from the platform administrator:\n")
                    .append(config.additionalInstructions().strip())
                    .append('\n');
        }

        return prompt.toString();
    }

    /** "4 technical questions" reads better to the model than "4 TECHNICAL questions". */
    private String describe(InterviewQuestionType type) {
        return type.name().toLowerCase().replace('_', ' ');
    }

    private String joinTypes(Set<InterviewQuestionType> types) {
        StringJoiner joiner = new StringJoiner(", ");
        types.forEach(type -> joiner.add(type.name()));
        return joiner.toString();
    }

    private void validate(GeneratedQuestionSet result, AiInterviewGenerationConfig config) {
        if (result == null || result.questions() == null) {
            throw new GeminiGenerationException(
                    "Gemini returned no questions"
            );
        }

        if (result.questions().size() != config.questionCount()) {
            throw new GeminiGenerationException(
                    "Gemini did not generate exactly %d questions".formatted(config.questionCount())
            );
        }

        Set<Integer> orders = new HashSet<>();
        Set<String> questionTexts = new HashSet<>();
        Map<InterviewQuestionType, Integer> generatedByType = new EnumMap<>(InterviewQuestionType.class);

        for (GeneratedQuestion question : result.questions()) {
            validateQuestion(question, config);

            if (!orders.add(question.order())) {
                throw new GeminiGenerationException(
                        "Gemini generated duplicate question orders"
                );
            }

            String normalized = question.question()
                    .trim()
                    .toLowerCase();

            if (!questionTexts.add(normalized)) {
                throw new GeminiGenerationException(
                        "Gemini generated duplicate questions"
                );
            }

            generatedByType.merge(question.type(), 1, Integer::sum);
        }

        validateTypeDistribution(generatedByType, config);
    }

    /**
     * A wrong mix is a weaker interview, not an unusable one, so it is logged
     * rather than thrown: failing here would cost the candidate the whole
     * generation over an imbalance a human reviewer would not notice.
     */
    private void validateTypeDistribution(
            Map<InterviewQuestionType, Integer> generated,
            AiInterviewGenerationConfig config
    ) {
        config.typeDistribution().forEach((type, expected) -> {
            int actual = generated.getOrDefault(type, 0);
            if (actual != expected) {
                log.warn(
                        "Gemini generated {} {} questions; {} were configured",
                        actual, type, expected
                );
            }
        });
    }

    private void validateQuestion(GeneratedQuestion question, AiInterviewGenerationConfig config) {
        if (question == null
                || question.order() == null
                || question.type() == null
                || question.question() == null
                || question.question().isBlank()
                || question.rubric() == null
                || question.rubric().isBlank()
                || question.maxScore() == null
                || question.maxScore() != config.maxScorePerQuestion()) {
            throw new GeminiGenerationException(
                    "Gemini generated an invalid question"
            );
        }
    }
}
