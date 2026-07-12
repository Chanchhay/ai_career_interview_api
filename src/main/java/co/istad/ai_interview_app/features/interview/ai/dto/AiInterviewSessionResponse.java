package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AiInterviewSessionResponse(
        Long id,
        Long jobId,
        String jobTitle,
        InterviewStatus status,
        Instant startedAt,
        Instant endedAt,
        BigDecimal totalScore,
        InterviewResult result,
        Integer questionCount,
        Integer answeredCount,
        List<AiInterviewQuestionResponse> questions
) {
}
