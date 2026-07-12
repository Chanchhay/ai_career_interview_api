package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestion;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestionSet;
import co.istad.ai_interview_app.shared.exception.GeminiGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AiInterviewQuestionGeneratorImpl
        implements AiInterviewQuestionGenerator {

    private static final int QUESTION_COUNT = 7;

    private final ChatClient geminiChatClient;

    @Override
    public GeneratedQuestionSet generateQuestions(
            String jobTitle,
            String jobDescription,
            String experienceLevel,
            List<String> requiredSkills
    ) {
        GeneratedQuestionSet result = geminiChatClient
                .prompt()
                .system("""
                        You are a professional technical interviewer.

                        Generate interview questions that assess whether a
                        candidate is suitable for the specified job.

                        Requirements:
                        - Generate exactly 7 questions.
                        - Include 4 technical questions.
                        - Include 2 behavioral questions.
                        - Include 1 situational question.
                        - Adapt difficulty to the experience level.
                        - Avoid duplicate questions.
                        - Include a private grading rubric.
                        - Set maxScore to 10 for every question.
                        """)
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

        validate(result);

        return result;
    }

    private void validate(GeneratedQuestionSet result) {
        if (result == null || result.questions() == null) {
            throw new GeminiGenerationException(
                    "Gemini returned no questions"
            );
        }

        if (result.questions().size() != QUESTION_COUNT) {
            throw new GeminiGenerationException(
                    "Gemini did not generate exactly 7 questions"
            );
        }

        Set<Integer> orders = new HashSet<>();
        Set<String> questionTexts = new HashSet<>();

        for (GeneratedQuestion question : result.questions()) {
            validateQuestion(question);

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
        }
    }

    private void validateQuestion(GeneratedQuestion question) {
        if (question == null
                || question.order() == null
                || question.type() == null
                || question.question() == null
                || question.question().isBlank()
                || question.rubric() == null
                || question.rubric().isBlank()
                || question.maxScore() == null
                || question.maxScore() != 10) {
            throw new GeminiGenerationException(
                    "Gemini generated an invalid question"
            );
        }
    }
}