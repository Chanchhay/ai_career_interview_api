package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * How many questions of one type an interview should contain. The admin console
 * edits a list of these; their counts add up to the interview's question count.
 */
public record QuestionTypeAllocation(

        @NotNull(message = "Question type is required")
        InterviewQuestionType type,

        @NotNull(message = "Question count is required")
        @Min(value = 0, message = "Question count cannot be negative")
        @Max(value = 30, message = "A single type may not exceed 30 questions")
        Integer count
) {
}
