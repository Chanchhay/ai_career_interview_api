package co.istad.ai_interview_app.features.interview.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * What the admin console sends when it changes how AI interviews are generated.
 *
 * <p>{@code questionCount} must equal the sum of {@code typeDistribution}; the
 * console computes it, and the service rejects a mismatch rather than silently
 * picking one of the two.
 */
public record AiInterviewConfigRequest(

        @NotNull(message = "Question count is required")
        @Min(value = 1, message = "An interview needs at least 1 question")
        @Max(value = 30, message = "An interview may not exceed 30 questions")
        Integer questionCount,

        @NotNull(message = "Max score per question is required")
        @Min(value = 1, message = "Max score per question must be at least 1")
        @Max(value = 100, message = "Max score per question may not exceed 100")
        Integer maxScorePerQuestion,

        @NotEmpty(message = "At least one question type is required")
        @Valid
        List<QuestionTypeAllocation> typeDistribution,

        @Size(max = 2000, message = "Extra instructions must be at most 2000 characters")
        String additionalInstructions
) {
}
