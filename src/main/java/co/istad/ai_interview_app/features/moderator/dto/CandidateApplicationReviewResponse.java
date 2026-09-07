package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record CandidateApplicationReviewResponse(
        UUID id,
        CandidateApplicationReviewStatus reviewStatus,
        String decisionNote,
        Instant reviewedAt,
        Instant approvedAt,
        Instant forwardedAt
) {
}
