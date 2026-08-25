package co.istad.ai_interview_app.features.interview.question;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetRequest;
import co.istad.ai_interview_app.features.interview.question.dto.JobInterviewQuestionSetResponse;
import co.istad.ai_interview_app.features.interview.question.service.JobInterviewQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Writing a job's interview questions by hand.
 *
 * <p>Sits under {@code /api/v1/admin/**}, so moderators reach it and
 * SUPER_ADMIN reaches it through the role hierarchy — the same people who
 * already curate the shared question mix. Recruiters cannot: the questions
 * decide whether a candidate passes, and the company doing the hiring is not a
 * neutral party to that.
 */
@RestController
@RequestMapping("/api/v1/admin/jobs/{jobId}/interview-questions")
@RequiredArgsConstructor
public class AdminJobInterviewQuestionController {

    private final JobInterviewQuestionService questionService;

    @GetMapping
    public ApiResponse<JobInterviewQuestionSetResponse> getSet(
            @PathVariable Long jobId
    ) {
        return ApiResponse.success(questionService.getSet(jobId));
    }

    /** Replaces the whole set. List order is the order candidates are asked. */
    @PutMapping
    public ApiResponse<JobInterviewQuestionSetResponse> saveSet(
            @PathVariable Long jobId,
            @Valid @RequestBody JobInterviewQuestionSetRequest request
    ) {
        return ApiResponse.success(questionService.saveSet(jobId, request));
    }
}
