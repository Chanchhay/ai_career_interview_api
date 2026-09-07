package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.moderation.ModerationDecision;

import java.time.Instant;
import java.util.UUID;

public record CompanyVerificationResponse(
        UUID id,
        UUID companyId,
        UUID moderatorProfileId,
        ModerationDecision decision,
        String note,
        Instant verifiedAt
) {
}
