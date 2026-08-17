package co.istad.ai_interview_app.features.job;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;
import co.istad.ai_interview_app.features.job.service.RecruiterSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a recruiter name a skill their job needs that nobody has entered yet.
 *
 * <p>The skills table is otherwise curated by admins, but a job post is worth
 * little without the technologies it asks for, and waiting on a moderator to
 * add "Zustand" would mean posting the job without it. Created skills are live
 * immediately; admins can rename or remove them through the tools they already
 * have.
 *
 * <p>Not a plain create: an existing skill with the same name, in any casing,
 * is returned as-is. See {@link RecruiterSkillService#findOrCreate}.
 */
@RestController
@RequestMapping("/api/v1/recruiter/skills")
@RequiredArgsConstructor
public class RecruiterSkillController {

    private final RecruiterSkillService recruiterSkillService;

    @PostMapping
    public ApiResponse<SkillResponse> findOrCreateSkill(
            @Valid @RequestBody SkillCreateRequest request
    ) {
        return ApiResponse.success(recruiterSkillService.findOrCreate(request));
    }
}
