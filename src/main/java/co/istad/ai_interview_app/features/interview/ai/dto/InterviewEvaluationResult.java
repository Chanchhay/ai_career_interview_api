package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;

import java.math.BigDecimal;
import java.util.List;

public record InterviewEvaluationResult(
        List<EvaluatedAnswer> answers,
        BigDecimal communicationScore,
        BigDecimal technicalScore,
        BigDecimal confidenceScore,
        BigDecimal problemSolvingScore,
        BigDecimal overallScore,
        String strengths,
        String weaknesses,
        String recommendation,
        InterviewResult result
) {
}
