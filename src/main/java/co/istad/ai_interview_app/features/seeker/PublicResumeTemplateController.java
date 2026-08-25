package co.istad.ai_interview_app.features.seeker;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.seeker.dto.PublicResumeTemplateResponse;
import co.istad.ai_interview_app.features.seeker.service.PublicResumeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The resume template catalog, readable signed out.
 *
 * <p>Public because the marketing site should be able to show what the builder
 * offers before anyone registers — the same reasoning as public job discovery.
 */
@RestController
@RequestMapping("/api/v1/public/resume-templates")
@RequiredArgsConstructor
public class PublicResumeTemplateController {

    private final PublicResumeTemplateService publicResumeTemplateService;

    @GetMapping
    public ApiResponse<List<PublicResumeTemplateResponse>> findTemplates() {
        return ApiResponse.success(publicResumeTemplateService.findActiveTemplates());
    }

    @GetMapping("/{templateId}")
    public ApiResponse<PublicResumeTemplateResponse> getTemplate(
            @PathVariable Long templateId
    ) {
        return ApiResponse.success(publicResumeTemplateService.getTemplate(templateId));
    }
}
