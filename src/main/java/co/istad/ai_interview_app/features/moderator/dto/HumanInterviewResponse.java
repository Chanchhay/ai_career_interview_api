package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;
import co.istad.ai_interview_app.shared.enums.interview.InterviewStatus;

import java.time.Instant;

public record HumanInterviewResponse(
        Long id,
        Long applicationId,
        Instant scheduledAt,
        String meetingUrl,
        InterviewStatus status,
        InterviewResult result,
        String note,
        Instant completedAt,
        Instant cancelledAt
) {
}
