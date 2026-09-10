package co.istad.ai_interview_app.features.interview.question;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.interview.question.dto.PublicJobInterviewPreviewResponse;
import co.istad.ai_interview_app.features.interview.question.service.JobInterviewQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

/**
 * What a published job's interview will ask, readable by anyone.
 *
 * <p>Open on purpose, like the job posting it belongs to: the point is to let
 * someone decide whether the interview is worth twenty minutes of their
 * evening, and a preview behind a login cannot do that. Both routes are GETs
 * under {@code /api/v1/public/**}, which the security config already permits.
 *
 * <p>The rubric never leaves {@link AdminJobInterviewQuestionController}. These
 * endpoints serve the questions only — enough to prepare for an interview, not
 * enough to game one.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicJobInterviewQuestionController {

    /**
     * The most jobs one batch will answer for. A page of the job board is 12,
     * so this leaves room without letting a caller ask for the whole table.
     */
    private static final int MAX_BATCH_SIZE = 50;

    private final JobInterviewQuestionService questionService;

    @GetMapping("/jobs/{jobId}/interview-questions")
    public ApiResponse<PublicJobInterviewPreviewResponse> getPreview(
            @PathVariable UUID jobId
    ) {
        return ApiResponse.success(questionService.getPublicPreview(jobId));
    }

    /**
     * Previews for a page of the board in one request, so a listing of twelve
     * jobs costs one round trip rather than twelve.
     *
     * <p>Sits at its own path rather than under {@code /jobs} so that
     * {@code interview-questions} is never mistaken for a job id.
     *
     * <p>Jobs with nothing written still come back, carrying an empty question
     * list — the caller decides what to do with that, and it is what tells the
     * screen the difference between "no questions" and "not asked yet".
     */
    @GetMapping("/job-interview-questions")
    public ApiResponse<List<PublicJobInterviewPreviewResponse>> getPreviews(
            @RequestParam List<UUID> jobIds
    ) {
        if (jobIds.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ask for at most " + MAX_BATCH_SIZE + " jobs at a time"
            );
        }

        return ApiResponse.success(questionService.getPublicPreviews(jobIds));
    }
}
