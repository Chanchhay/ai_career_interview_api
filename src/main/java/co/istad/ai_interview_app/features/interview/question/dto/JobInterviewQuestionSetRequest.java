package co.istad.ai_interview_app.features.interview.question.dto;

import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The complete set of hand-written questions for a job, saved in one write.
 *
 * <p>Whole-set rather than per-question endpoints because that is how the screen
 * is used: add, reword, reorder and drop, then save once. Order in the list is
 * the order candidates are asked, so reordering needs no separate call.
 */
public record JobInterviewQuestionSetRequest(

        @NotNull(message = "Choose whether the AI adds questions")
        ManualQuestionMode mode,

        @NotNull(message = "Send the questions, even if the list is empty")
        @Size(max = 50, message = "A job can hold at most 50 written questions")
        List<@Valid JobInterviewQuestionRequest> questions
) {
}
