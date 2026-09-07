package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiInterviewSessionResponse(
        UUID id,
        UUID applicationId,
        UUID jobId,
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
