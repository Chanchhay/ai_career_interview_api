package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;

import java.math.BigDecimal;

public record AiInterviewFeedbackResponse(
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
