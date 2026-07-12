package co.istad.ai_interview_app.features.interview.ai.mapper;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewFeedbackResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewQuestionResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewAnswer;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewFeedback;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewQuestion;
import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewSession;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AiInterviewMapper {

    public AiInterviewSessionResponse toSessionResponse(AiInterviewSession session) {
        List<AiInterviewQuestionResponse> questions = toQuestionResponses(session.getQuestions());

        return new AiInterviewSessionResponse(
                session.getId(),
                session.getJobPost().getId(),
                session.getJobPost().getTitle(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getTotalScore(),
                session.getResult(),
                session.getQuestions().size(),
                countAnswered(session.getQuestions()),
                questions
        );
    }

    public AiInterviewResultResponse toResultResponse(AiInterviewSession session) {
        return new AiInterviewResultResponse(
                toSessionResponse(session),
                toFeedbackResponse(session.getFeedback()),
                toQuestionResponses(session.getQuestions())
        );
    }

    private List<AiInterviewQuestionResponse> toQuestionResponses(List<AiInterviewQuestion> questions) {
        return questions.stream()
                .sorted(Comparator.comparing(AiInterviewQuestion::getDisplayOrder))
                .map(this::toQuestionResponse)
                .toList();
    }

    private AiInterviewQuestionResponse toQuestionResponse(AiInterviewQuestion question) {
        AiInterviewAnswer answer = question.getAnswers()
                .stream()
                .findFirst()
                .orElse(null);

        return new AiInterviewQuestionResponse(
                question.getId(),
                question.getDisplayOrder(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getMaxScore(),
                answer != null && answer.getAnswerText() != null && !answer.getAnswerText().isBlank(),
                toAnswerResponse(answer)
        );
    }

    private AiInterviewAnswerResponse toAnswerResponse(AiInterviewAnswer answer) {
        if (answer == null) {
            return null;
        }

        return new AiInterviewAnswerResponse(
                answer.getId(),
                answer.getAnswerText(),
                answer.getScore(),
                answer.getFeedback()
        );
    }

    private AiInterviewFeedbackResponse toFeedbackResponse(AiInterviewFeedback feedback) {
        if (feedback == null) {
            return null;
        }

        return new AiInterviewFeedbackResponse(
                feedback.getCommunicationScore(),
                feedback.getTechnicalScore(),
                feedback.getConfidenceScore(),
                feedback.getProblemSolvingScore(),
                feedback.getOverallScore(),
                feedback.getStrengths(),
                feedback.getWeaknesses(),
                feedback.getRecommendation(),
                feedback.getSession().getResult()
        );
    }

    private int countAnswered(List<AiInterviewQuestion> questions) {
        return (int) questions.stream()
                .filter(question -> question.getAnswers()
                        .stream()
                        .anyMatch(answer -> answer.getAnswerText() != null && !answer.getAnswerText().isBlank()))
                .count();
    }
}
