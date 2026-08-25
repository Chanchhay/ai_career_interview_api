package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.application.repository.JobApplicationRepository;
import co.istad.ai_interview_app.features.file.FileVisibility;
import co.istad.ai_interview_app.features.file.dto.DownloadedFile;
import co.istad.ai_interview_app.features.file.dto.FileUploadResponse;
import co.istad.ai_interview_app.features.file.service.FileStorageService;
import co.istad.ai_interview_app.features.seeker.pdf.ResumePdfRenderer;
import co.istad.ai_interview_app.shared.enums.seeker.ResumeSourceType;
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
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final ResumePdfRenderer resumePdfRenderer;
    private final FileStorageService fileStorageService;

    private static final String DOCX_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /**
     * Resume uploads accept more than the shared file endpoint does, so the
     * allow-list lives here rather than widening {@code /api/v1/files} for
     * everyone.
     */
    private static final Map<String, String> UPLOAD_TYPES = Map.of(
            MediaType.APPLICATION_PDF_VALUE, "pdf",
            DOCX_TYPE, "docx"
    );

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


    /* ------------------------------------------------------------ files --- */

    /**
     * Renders the resume and replaces its stored PDF.
     *
     * <p>The previous object is deleted only after the new one is stored and the
     * row updated. Losing the old file before the new one exists would leave a
     * resume with a link to nothing; an orphan in the bucket is the cheaper
     * failure.
     */
    @Override
    @Transactional
    public ResumeResponse generate(Long resumeId) {
        JobSeekerProfile profile = profileResolver.resolve();
        Resume resume = resolveOwnedResume(resumeId, profile.getId());

        if (resume.getSourceType() == ResumeSourceType.USER_UPLOAD) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An uploaded resume is stored as supplied and cannot be regenerated"
            );
        }

        if (resume.getResumeData() == null || resume.getResumeData().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Add some content to the resume before generating a PDF"
            );
        }

        String previousKey = keyOf(resume.getResumeFileUrl());

        byte[] pdf = resumePdfRenderer.render(resume.getResumeData());
        FileUploadResponse stored = fileStorageService.store(
                pdf,
                "pdf",
                MediaType.APPLICATION_PDF_VALUE,
                FileVisibility.PRIVATE
        );

        resume.setResumeFileUrl(stored.url());
        resume.setGeneratedAt(Instant.now());
        resume.setFileVersion(resume.getFileVersion() == null ? 1 : resume.getFileVersion() + 1);

        if (previousKey != null) {
            fileStorageService.delete(previousKey);
        }

        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional
    public ResumeResponse uploadOwnResume(String title, MultipartFile file) {
        JobSeekerProfile profile = profileResolver.resolve();

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }

        String extension = UPLOAD_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "A resume must be a PDF or a DOCX file"
            );
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }

        FileUploadResponse stored = fileStorageService.store(
                content,
                extension,
                file.getContentType(),
                FileVisibility.PRIVATE
        );

        Resume resume = new Resume();
        resume.setJobSeekerProfile(profile);
        resume.setTitle(normalizeRequiredText(title, "Title is required"));
        resume.setResumeFileUrl(stored.url());
        /*
         * No resumeData, and none inferred. The platform does not parse an
         * uploaded resume — that scope limit is deliberate, and leaving the
         * column null is what makes it visible rather than implied.
         */
        resume.setSourceType(ResumeSourceType.USER_UPLOAD);
        resume.setIsDefault(false);
        resume.setVisibility(VisibilityStatus.PRIVATE);
        resume.setFileVersion(1);

        return resumeMapper.toResponse(resumeRepository.save(resume));
    }

    /**
     * Reads the file back for the owner.
     *
     * <p>Streams through the backend rather than handing out a presigned URL:
     * the object key is derivable from a response the owner already has, and a
     * link that keeps working for its whole expiry window is a wider grant than
     * "this person asked for their own file just now".
     */
    @Override
    @Transactional(readOnly = true)
    public DownloadedFile download(Long resumeId) {
        JobSeekerProfile profile = profileResolver.resolve();
        Resume resume = resolveOwnedResume(resumeId, profile.getId());

        String storedUrl = resume.getResumeFileUrl();

        if (storedUrl == null || storedUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "This resume has no file yet. Generate one first."
            );
        }

        String key = keyOf(storedUrl);

        // Resumes created before object storage hold a URL the user supplied.
        // There are no bytes here to stream, so the browser is sent there.
        if (key == null) {
            return DownloadedFile.redirect(storedUrl);
        }

        String extension = key.endsWith(".docx") ? "docx" : "pdf";
        String contentType = "docx".equals(extension) ? DOCX_TYPE : MediaType.APPLICATION_PDF_VALUE;

        return DownloadedFile.streamed(
                fileStorageService.read(key),
                "%s.%s".formatted(safeFilename(resume.getTitle()), extension),
                contentType
        );
    }

    /**
     * The object key inside a stored app-relative URL, or null when the resume
     * points at something this application did not store — older rows hold
     * arbitrary external links.
     */
    private String keyOf(String resumeFileUrl) {
        if (resumeFileUrl == null || resumeFileUrl.isBlank()) return null;

        for (String prefix : List.of("/api/v1/files/", "/api/v1/public/files/")) {
            if (resumeFileUrl.startsWith(prefix)) {
                return resumeFileUrl.substring(prefix.length());
            }
        }

        return null;
    }

    /** Keeps a user-chosen title from steering the Content-Disposition header. */
    private String safeFilename(String title) {
        String cleaned = title == null ? "" : title.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        return cleaned.isBlank() ? "resume" : cleaned.replace(' ', '-');
    }

    private String normalizeRequiredText(String value, String message) {
        if (!hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
