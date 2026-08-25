package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.PublicResumeTemplateResponse;
import co.istad.ai_interview_app.features.seeker.entity.ResumeTemplate;
import co.istad.ai_interview_app.features.seeker.repository.ResumeTemplateRepository;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * The template catalog.
 *
 * <p>Only ACTIVE templates are ever returned, including from the by-id lookup:
 * a template an administrator has retired must not stay selectable through a
 * link someone kept.
 */
@Service
@RequiredArgsConstructor
public class PublicResumeTemplateServiceImpl implements PublicResumeTemplateService {

    private final ResumeTemplateRepository resumeTemplateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PublicResumeTemplateResponse> findActiveTemplates() {
        return resumeTemplateRepository.findAllByStatusOrderByNameAsc(ProfileStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicResumeTemplateResponse getTemplate(Long templateId) {
        return resumeTemplateRepository.findByIdAndStatus(templateId, ProfileStatus.ACTIVE)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume template was not found"
                ));
    }

    private PublicResumeTemplateResponse toResponse(ResumeTemplate template) {
        Map<String, Object> schema = template.getTemplateSchema() == null
                ? Map.of()
                : template.getTemplateSchema();

        return new PublicResumeTemplateResponse(
                template.getId(),
                asText(schema.get("templateKey")),
                template.getName(),
                asText(schema.get("description")),
                template.getPreviewImageUrl(),
                schema
        );
    }

    private String asText(Object value) {
        return value == null ? null : value.toString();
    }
}
