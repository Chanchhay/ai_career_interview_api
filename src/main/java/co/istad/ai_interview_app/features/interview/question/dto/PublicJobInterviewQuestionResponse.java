package co.istad.ai_interview_app.features.interview.question.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import java.util.UUID;

/**
 * One written question as a visitor sees it before the interview starts.
 *
 * <p>Deliberately thinner than {@link JobInterviewQuestionResponse}: it drops
 * {@code expectedAnswer}. That field is the scoring rubric, and this record is
 * served to anyone at all — publishing it would hand every candidate the mark
 * scheme for the interview they are about to sit.
 */
public record PublicJobInterviewQuestionResponse(
        UUID id,
        Integer displayOrder,
        InterviewQuestionType questionType,
        String questionText,
        Integer maxScore
) {
}
