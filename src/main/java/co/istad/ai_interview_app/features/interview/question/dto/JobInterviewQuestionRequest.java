package co.istad.ai_interview_app.features.interview.question.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One question in a saved set.
 *
 * <p>{@code id} names a question already on the job, which is then updated in
 * place. Leave it null for a new one. Anything the job holds that the request
 * does not name is deleted — the request is the whole set, not a patch.
 */
public record JobInterviewQuestionRequest(
        Long id,

        @NotBlank(message = "A question needs text")
        @Size(max = 2000, message = "A question must be 2000 characters or fewer")
        String questionText,

        @NotNull(message = "A question needs a type")
        InterviewQuestionType questionType,

        @Size(max = 2000, message = "The expected answer must be 2000 characters or fewer")
        String expectedAnswer,

        /** Null takes the platform's configured score per question. */
        @Min(value = 1, message = "A question must be worth at least 1 point")
        @Max(value = 100, message = "A question must be worth 100 points or fewer")
        Integer maxScore
) {
}
