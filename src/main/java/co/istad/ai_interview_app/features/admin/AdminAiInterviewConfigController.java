package co.istad.ai_interview_app.features.admin;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewConfigRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewConfigResponse;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The knobs behind AI interview generation: interview length, the mix of
 * question types, what a question is worth, and any extra wording for the
 * prompt.
 *
 * <p>Lives under {@code /api/v1/admin/**}, so it is reachable by MODERATOR and
 * above — the same rule as the shared taxonomy. See {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/ai-interview-config")
@RequiredArgsConstructor
public class AdminAiInterviewConfigController {

    private final AiInterviewConfigService aiInterviewConfigService;

    @GetMapping
    public ApiResponse<AiInterviewConfigResponse> getConfig() {
        return ApiResponse.success(aiInterviewConfigService.getConfig());
    }

    @PutMapping
    public ApiResponse<AiInterviewConfigResponse> updateConfig(
            @Valid @RequestBody AiInterviewConfigRequest request
    ) {
        return ApiResponse.success(aiInterviewConfigService.updateConfig(request));
    }
}
