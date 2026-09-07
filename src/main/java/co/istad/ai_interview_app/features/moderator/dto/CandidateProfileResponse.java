package co.istad.ai_interview_app.features.moderator.dto;

import java.util.UUID;

public record CandidateProfileResponse(
        UUID id,
        String headline,
        String currentPosition,
        String preferredLocation,
        String availabilityStatus
) {
}
