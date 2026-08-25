package co.istad.ai_interview_app.features.recruiter.talent.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicPortfolioProjectResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicPortfolioResponse;
import co.istad.ai_interview_app.features.file.dto.DownloadedFile;
import co.istad.ai_interview_app.features.file.service.FileStorageService;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicResumeResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicTalentDetailResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicTalentListItemResponse;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Portfolio;
import co.istad.ai_interview_app.features.seeker.entity.PortfolioProject;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.features.seeker.repository.JobSeekerProfileRepository;
import co.istad.ai_interview_app.features.seeker.repository.PortfolioProjectRepository;
import co.istad.ai_interview_app.features.seeker.repository.PortfolioRepository;
import co.istad.ai_interview_app.features.seeker.repository.ResumeRepository;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.profile.SalaryVisibility;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import co.istad.ai_interview_app.features.seeker.specification.JobSeekerProfileSpecification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class RecruiterTalentServiceImpl implements RecruiterTalentService {

    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioProjectRepository portfolioProjectRepository;
    private final ResumeRepository resumeRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<PublicTalentListItemResponse> findPublicTalent(
            String keyword,
            String preferredLocation,
            String availabilityStatus,
            Pageable pageable
    ) {
        Specification<JobSeekerProfile> spec = JobSeekerProfileSpecification.filterPublicTalent(
                ProfileStatus.ACTIVE,
                VisibilityStatus.PUBLIC,
                normalizeBlankToNull(keyword),
                normalizeBlankToNull(preferredLocation),
                normalizeBlankToNull(availabilityStatus)
        );

        return jobSeekerProfileRepository.findAll(spec, pageable)
                .map(this::toListItemResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicTalentDetailResponse getPublicTalent(String publicProfileSlug) {
        JobSeekerProfile profile = resolvePublicProfile(publicProfileSlug);

        List<PublicPortfolioResponse> portfolios = portfolioRepository
                .findAllByJobSeekerProfile_IdAndStatusAndVisibilityOrderByCreatedAtDesc(
                        profile.getId(),
                        ProfileStatus.ACTIVE,
                        VisibilityStatus.PUBLIC
                )
                .stream()
                .map(this::toPortfolioResponse)
                .toList();

        List<PublicResumeResponse> resumes = resumeRepository
                .findAllByJobSeekerProfile_IdAndVisibilityOrderByPublishedAtDescCreatedAtDesc(
                        profile.getId(),
                        VisibilityStatus.PUBLIC
                )
                .stream()
                .map(this::toResumeResponse)
                .toList();

        return new PublicTalentDetailResponse(
                toListItemResponse(profile),
                portfolios,
                resumes
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedFile getPublicResumeDownload(String publicProfileSlug, Long resumeId) {
        if (!AuthUtils.hasRole(AuthUtils.extractJwtAuthentication().getAuthorities(), "RECRUITER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recruiter role is required");
        }

        JobSeekerProfile profile = resolvePublicProfile(publicProfileSlug);
        Resume resume = resumeRepository.findByIdAndJobSeekerProfile_Id(resumeId, profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public resume was not found"));

        if (resume.getVisibility() != VisibilityStatus.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Public resume was not found");
        }

        /*
         * Streamed rather than handed back as a URL. The stored value points at
         * /api/v1/files/{key}, which any authenticated caller can read — so
         * returning it turned a published-resume check into a link that any
         * signed-in account could reuse. Reading the bytes here keeps the
         * publication and ownership checks above on every single fetch.
         */
        String storedUrl = resume.getResumeFileUrl();

        if (storedUrl == null || storedUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "This resume has no downloadable file"
            );
        }

        String key = storedKey(storedUrl);

        // A resume whose file was never stored here — an older row holding a
        // client-supplied link. Nothing to stream, so the browser is sent there.
        if (key == null) {
            return DownloadedFile.redirect(storedUrl);
        }

        boolean docx = key.endsWith(".docx");

        return DownloadedFile.streamed(
                fileStorageService.read(key),
                "%s.%s".formatted(safeFilename(resume.getTitle()), docx ? "docx" : "pdf"),
                docx
                        ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        : MediaType.APPLICATION_PDF_VALUE
        );
    }

    /** The object key inside a stored app-relative URL, or null for anything else. */
    private String storedKey(String resumeFileUrl) {
        if (resumeFileUrl == null || resumeFileUrl.isBlank()) return null;

        for (String prefix : List.of("/api/v1/files/", "/api/v1/public/files/")) {
            if (resumeFileUrl.startsWith(prefix)) {
                return resumeFileUrl.substring(prefix.length());
            }
        }

        return null;
    }

    /** Keeps a candidate-chosen title from steering the Content-Disposition header. */
    private String safeFilename(String title) {
        String cleaned = title == null ? "" : title.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        return cleaned.isBlank() ? "resume" : cleaned.replace(' ', '-');
    }

    private JobSeekerProfile resolvePublicProfile(String publicProfileSlug) {
        return jobSeekerProfileRepository.findByPublicProfileSlugAndStatusAndProfileVisibility(
                        publicProfileSlug,
                        ProfileStatus.ACTIVE,
                        VisibilityStatus.PUBLIC
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public talent profile was not found"));
    }

    private PublicTalentListItemResponse toListItemResponse(JobSeekerProfile profile) {
        boolean canShowSalary = profile.getSalaryVisibility() == SalaryVisibility.PUBLIC
                || profile.getSalaryVisibility() == SalaryVisibility.RECRUITERS_ONLY;

        return new PublicTalentListItemResponse(
                profile.getId(),
                profile.getPublicProfileSlug(),
                profile.getAvatarUrl(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getCurrentPosition(),
                profile.getPreferredLocation(),
                profile.getAvailabilityStatus(),
                canShowSalary ? profile.getExpectedSalaryMin() : null,
                canShowSalary ? profile.getExpectedSalaryMax() : null,
                canShowSalary ? profile.getExpectedSalaryCurrency() : null,
                profile.getSalaryVisibility()
        );
    }

    private PublicPortfolioResponse toPortfolioResponse(Portfolio portfolio) {
        List<PublicPortfolioProjectResponse> projects = portfolioProjectRepository
                .findAllByPortfolio_IdOrderByDisplayOrderAscCreatedAtDesc(portfolio.getId())
                .stream()
                .map(this::toProjectResponse)
                .toList();

        return new PublicPortfolioResponse(
                portfolio.getId(),
                portfolio.getTitle(),
                portfolio.getSummary(),
                portfolio.getPublicUrl(),
                portfolio.getPortfolioData(),
                portfolio.getPublishedAt(),
                projects
        );
    }

    private PublicPortfolioProjectResponse toProjectResponse(PortfolioProject project) {
        return new PublicPortfolioProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getProjectUrl(),
                project.getGithubUrl(),
                project.getImageUrl(),
                project.getTechStack(),
                project.getDisplayOrder()
        );
    }

    private PublicResumeResponse toResumeResponse(Resume resume) {
        return new PublicResumeResponse(
                resume.getId(),
                resume.getTitle(),
                resume.getIsDefault(),
                resume.getResumeFileUrl(),
                resume.getResumeData(),
                resume.getPublishedAt()
        );
    }
}
