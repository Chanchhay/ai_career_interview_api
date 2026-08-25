package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.PublicResumeTemplateResponse;

import java.util.List;

public interface PublicResumeTemplateService {

    List<PublicResumeTemplateResponse> findActiveTemplates();

    PublicResumeTemplateResponse getTemplate(Long templateId);
}
