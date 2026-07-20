package co.istad.ai_interview_app.features.moderator.dto;

public record CandidateProfileResponse(
        Long id,
        String headline,
        String currentPosition,
        String preferredLocation,
        String availabilityStatus
) {
}
