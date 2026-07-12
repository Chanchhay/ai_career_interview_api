package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.EvaluatedAnswer;
import co.istad.ai_interview_app.features.interview.ai.dto.InterviewEvaluationRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.InterviewEvaluationResult;
import co.istad.ai_interview_app.shared.exception.GeminiGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AiInterviewEvaluatorImpl implements AiInterviewEvaluator {

    private final ChatClient geminiChatClient;

    @Override
    public InterviewEvaluationResult evaluate(InterviewEvaluationRequest request) {
        InterviewEvaluationResult result = geminiChatClient
                .prompt()
                .system("""
                        You are a strict but fair technical interview evaluator.

                        Evaluate the candidate's text answers against the private
                        rubrics. Return one score and feedback per question, plus
                        overall feedback.

                        Requirements:
                        - Score each answer from 0 to that question's maxScore.
                        - Overall category scores must be from 0 to 10.
                        - overallScore must be from 0 to 10.
                        - Use PASSED, FAILED, or NEEDS_REVIEW for result.
                        - Feedback must be concise, specific, and actionable.
                        """)
                .user(user -> user
                        .text("""
                                Job title: {jobTitle}

                                Experience level: {experienceLevel}

                                Required skills:
                                {requiredSkills}

                                Job description:
                                {jobDescription}

                                Candidate answers with private rubrics:
                                {answers}
                                """)
                        .param("jobTitle", request.jobTitle())
                        .param("experienceLevel", request.experienceLevel())
                        .param("requiredSkills", String.join(", ", request.requiredSkills()))
                        .param("jobDescription", request.jobDescription())
                        .param("answers", request.answers().toString())
                )
                .call()
                .entity(
                        InterviewEvaluationResult.class,
                        specification -> specification
                                .useProviderStructuredOutput()
                                .validateSchema()
                );

        validate(request, result);

        return result;
    }

    private void validate(
            InterviewEvaluationRequest request,
            InterviewEvaluationResult result
    ) {
        if (result == null || result.answers() == null) {
            throw new GeminiGenerationException("Gemini returned no interview evaluation");
        }

        if (result.answers().size() != request.answers().size()) {
            throw new GeminiGenerationException("Gemini did not evaluate every answer");
        }

        Set<Long> expectedQuestionIds = new HashSet<>();
        request.answers().forEach(answer -> expectedQuestionIds.add(answer.questionId()));

        Set<Long> evaluatedQuestionIds = new HashSet<>();
        for (EvaluatedAnswer answer : result.answers()) {
            validateAnswer(answer, expectedQuestionIds);
            evaluatedQuestionIds.add(answer.questionId());
        }

        if (!evaluatedQuestionIds.equals(expectedQuestionIds)) {
            throw new GeminiGenerationException("Gemini evaluated unexpected questions");
        }

        validateScore(result.communicationScore(), "communicationScore");
        validateScore(result.technicalScore(), "technicalScore");
        validateScore(result.confidenceScore(), "confidenceScore");
        validateScore(result.problemSolvingScore(), "problemSolvingScore");
        validateScore(result.overallScore(), "overallScore");

        if (result.result() == null
                || isBlank(result.strengths())
                || isBlank(result.weaknesses())
                || isBlank(result.recommendation())) {
            throw new GeminiGenerationException("Gemini generated incomplete overall feedback");
        }
    }

    private void validateAnswer(
            EvaluatedAnswer answer,
            Set<Long> expectedQuestionIds
    ) {
        if (answer == null
                || answer.questionId() == null
                || !expectedQuestionIds.contains(answer.questionId())
                || answer.feedback() == null
                || answer.feedback().isBlank()) {
            throw new GeminiGenerationException("Gemini generated an invalid answer evaluation");
        }

        validateScore(answer.score(), "answer score");
    }

    private void validateScore(BigDecimal score, String fieldName) {
        if (score == null
                || score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(BigDecimal.TEN) > 0) {
            throw new GeminiGenerationException("Gemini generated an invalid " + fieldName);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
