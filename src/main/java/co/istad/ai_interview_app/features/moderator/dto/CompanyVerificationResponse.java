package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.moderation.ModerationDecision;

import java.time.Instant;

public record CompanyVerificationResponse(
        Long id,
        Long companyId,
        Long moderatorProfileId,
        ModerationDecision decision,
        String note,
        Instant verifiedAt
) {
}
