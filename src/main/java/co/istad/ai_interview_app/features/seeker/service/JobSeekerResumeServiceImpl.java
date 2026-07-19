package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.application.repository.JobApplicationRepository;
import co.istad.ai_interview_app.features.seeker.dto.ResumeCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.ResumeResponse;
import co.istad.ai_interview_app.features.seeker.dto.ResumeUpdateRequest;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.features.seeker.mapper.ResumeMapper;
import co.istad.ai_interview_app.features.seeker.repository.ResumeRepository;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class JobSeekerResumeServiceImpl implements JobSeekerResumeService {

    private final AuthenticatedJobSeekerProfileResolver profileResolver;
    private final ResumeRepository resumeRepository;
    private final JobApplicationRepository applicationRepository;
    private final ResumeMapper resumeMapper;

    @Override
    @Transactional
    public ResumeResponse create(ResumeCreateRequest request) {
        JobSeekerProfile profile = profileResolver.resolve();

        Resume resume = new Resume();
        resume.setJobSeekerProfile(profile);
        resume.setTitle(normalizeRequiredText(request.title(), "Title is required"));
        resume.setResumeFileUrl(normalizeBlankToNull(request.resumeFileUrl()));
        resume.setResumeData(request.resumeData());
        resume.setIsDefault(false);
        resume.setVisibility(VisibilityStatus.PRIVATE);

        return resumeMapper.toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getMyResumes() {
        JobSeekerProfile profile = profileResolver.resolve();

        return resumeRepository.findAllByJobSeekerProfile_IdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(resumeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getMyResume(Long resumeId) {
        JobSeekerProfile profile = profileResolver.resolve();
        return resumeMapper.toResponse(resolveOwnedResume(resumeId, profile.getId()));
    }

    @Override
    @Transactional
    public ResumeResponse update(Long resumeId, ResumeUpdateRequest request) {
        JobSeekerProfile profile = profileResolver.resolve();
        Resume resume = resolveOwnedResume(resumeId, profile.getId());

        if (request.title() != null) {
            resume.setTitle(normalizeRequiredText(request.title(), "Title is required"));
        }
        if (request.resumeFileUrl() != null) {
            String fileUrl = normalizeBlankToNull(request.resumeFileUrl());
            if (!Objects.equals(fileUrl, resume.getResumeFileUrl())
                    && applicationRepository.existsByResume_Id(resume.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Resume file URL cannot be changed after the resume is referenced by an application"
                );
            }
            resume.setResumeFileUrl(fileUrl);
        }
        if (request.resumeData() != null) {
            resume.setResumeData(request.resumeData());
        }

        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional
    public void delete(Long resumeId) {
        JobSeekerProfile profile = profileResolver.resolve();
        Resume resume = resolveOwnedResume(resumeId, profile.getId());

        if (applicationRepository.existsByResume_Id(resume.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Resume cannot be deleted because it is referenced by an application"
            );
        }

        resumeRepository.delete(resume);
    }

    @Override
    @Transactional
    public ResumeResponse setDefault(Long resumeId) {
        JobSeekerProfile profile = profileResolver.resolve();
        Resume resume = resolveOwnedResume(resumeId, profile.getId());

        resumeRepository.clearDefaultForJobSeekerProfile(profile.getId());
        resume.setIsDefault(true);

        return resumeMapper.toResponse(resume);
    }

    private Resume resolveOwnedResume(Long resumeId, Long profileId) {
        return resumeRepository.findByIdAndJobSeekerProfile_Id(resumeId, profileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume was not found for authenticated job seeker"
                ));
    }

    private String normalizeRequiredText(String value, String message) {
        if (!hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
