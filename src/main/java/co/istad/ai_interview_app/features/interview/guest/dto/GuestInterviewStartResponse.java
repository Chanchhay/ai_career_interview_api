package co.istad.ai_interview_app.features.interview.guest.dto;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;

/**
 * A started guest interview, plus the token the browser must keep.
 *
 * <p>The token comes back only here, on the request that creates the interview.
 * It is what proves the interview is this visitor's, so losing it means losing
 * the interview — there is no account to recover it from.
 */
public record GuestInterviewStartResponse(
        String guestToken,
        int attemptsUsed,
        int attemptsAllowed,
        AiInterviewSessionResponse session
) {
}
