package co.istad.ai_interview_app.features.interview.guest.dto;

/**
 * What the public site needs to decide whether to offer a guest interview at
 * all, without starting one.
 *
 * <p>Answered for a visitor who may have no token yet, so it never fails: an
 * unknown guest has simply used nothing.
 */
public record GuestInterviewAvailabilityResponse(
        boolean enabled,
        int attemptsUsed,
        int attemptsAllowed,
        boolean canStart,
        /** Why not, in words a visitor can read, or null when they can start. */
        String blockedReason
) {
}
