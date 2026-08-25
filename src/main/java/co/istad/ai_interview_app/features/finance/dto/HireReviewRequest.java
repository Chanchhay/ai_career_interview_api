package co.istad.ai_interview_app.features.finance.dto;

import jakarta.validation.constraints.Size;

/** A moderator's note when confirming or rejecting a reported hire. */
public record HireReviewRequest(
        @Size(max = 2000) String note
) {
}
