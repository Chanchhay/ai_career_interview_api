package co.istad.ai_interview_app.features.recruiter.talent.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicPortfolioProjectResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicPortfolioResponse;
import co.istad.ai_interview_app.features.recruiter.talent.dto.PublicResumeDownloadResponse;
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
import org.springframework.http.HttpStatus;
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

    @Override
    @Transactional(readOnly = true)
    public Page<PublicTalentListItemResponse> findPublicTalent(
            String keyword,
            String preferredLocation,
            String availabilityStatus,
            Pageable pageable
    ) {
        return jobSeekerProfileRepository.findPublicTalent(
                        ProfileStatus.ACTIVE,
                        VisibilityStatus.PUBLIC,
                        normalizeBlankToNull(keyword),
                        normalizeBlankToNull(preferredLocation),
                        normalizeBlankToNull(availabilityStatus),
                        pageable
                )
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
    public PublicResumeDownloadResponse getPublicResumeDownload(String publicProfileSlug, Long resumeId) {
        if (!AuthUtils.hasRole(AuthUtils.extractJwtAuthentication().getAuthorities(), "RECRUITER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recruiter role is required");
        }

        JobSeekerProfile profile = resolvePublicProfile(publicProfileSlug);
        Resume resume = resumeRepository.findByIdAndJobSeekerProfile_Id(resumeId, profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public resume was not found"));

        if (resume.getVisibility() != VisibilityStatus.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Public resume was not found");
        }

        return new PublicResumeDownloadResponse(resume.getId(), resume.getResumeFileUrl());
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
                resume.getPublishedAt()
        );
    }
}
