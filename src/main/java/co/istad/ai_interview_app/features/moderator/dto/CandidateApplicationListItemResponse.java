package co.istad.ai_interview_app.features.moderator.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewResult;

import java.math.BigDecimal;

/**
 * A row of the review queue.
 *
 * <p>Carries the AI outcome as well as the application itself: it is the first
 * thing a moderator looks at when deciding who to open, and fetching it per
 * row would be a request per candidate on a screen being scanned.
 */
public record CandidateApplicationListItemResponse(
        ApplicationSummaryResponse application,
        CandidateProfileResponse candidate,
        SubmittedResumeResponse submittedResume,
        CandidateApplicationReviewResponse review,
        /** Null until the candidate's AI interview has finished. */
        BigDecimal aiScore,
        InterviewResult aiResult
) {
}
