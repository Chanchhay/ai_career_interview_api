package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.PublicationRequest;
import co.istad.ai_interview_app.features.seeker.dto.PublicationResponse;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Portfolio;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.features.seeker.repository.PortfolioRepository;
import co.istad.ai_interview_app.features.seeker.repository.ResumeRepository;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

@Service
@RequiredArgsConstructor
public class JobSeekerPublicationServiceImpl implements JobSeekerPublicationService {

    private final AuthenticatedJobSeekerProfileResolver jobSeekerProfileResolver;
    private final PortfolioRepository portfolioRepository;
    private final ResumeRepository resumeRepository;

    @Override
    @Transactional
    public PublicationResponse updateProfilePublication(PublicationRequest request) {
        VisibilityStatus visibility = resolvePublicationVisibility(request);
        JobSeekerProfile profile = jobSeekerProfileResolver.resolve();

        profile.setProfileVisibility(visibility);
        if (visibility == VisibilityStatus.PUBLIC) {
            if (!hasText(profile.getPublicProfileSlug())) {
                profile.setPublicProfileSlug("talent-" + profile.getId());
            }
            if (profile.getPublishedAt() == null) {
                profile.setPublishedAt(Instant.now());
            }
        }

        return new PublicationResponse(
                "PROFILE",
                profile.getId(),
                profile.getProfileVisibility(),
                profile.getPublicProfileSlug(),
                profile.getPublishedAt()
        );
    }

    @Override
    @Transactional
    public PublicationResponse updatePortfolioPublication(Long portfolioId, PublicationRequest request) {
        VisibilityStatus visibility = resolvePublicationVisibility(request);
        JobSeekerProfile profile = jobSeekerProfileResolver.resolve();
        Portfolio portfolio = portfolioRepository.findByIdAndJobSeekerProfile_Id(portfolioId, profile.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portfolio was not found for authenticated job seeker"
                ));

        portfolio.setVisibility(visibility);
        if (visibility == VisibilityStatus.PUBLIC && portfolio.getPublishedAt() == null) {
            portfolio.setPublishedAt(Instant.now());
        }

        return new PublicationResponse(
                "PORTFOLIO",
                portfolio.getId(),
                portfolio.getVisibility(),
                profile.getPublicProfileSlug(),
                portfolio.getPublishedAt()
        );
    }

    @Override
    @Transactional
    public PublicationResponse updateResumePublication(Long resumeId, PublicationRequest request) {
        VisibilityStatus visibility = resolvePublicationVisibility(request);
        JobSeekerProfile profile = jobSeekerProfileResolver.resolve();
        Resume resume = resumeRepository.findByIdAndJobSeekerProfile_Id(resumeId, profile.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume was not found for authenticated job seeker"
                ));

        if (visibility == VisibilityStatus.PUBLIC && !hasText(resume.getResumeFileUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is required before publishing");
        }

        resume.setVisibility(visibility);
        if (visibility == VisibilityStatus.PUBLIC && resume.getPublishedAt() == null) {
            resume.setPublishedAt(Instant.now());
        }

        return new PublicationResponse(
                "RESUME",
                resume.getId(),
                resume.getVisibility(),
                profile.getPublicProfileSlug(),
                resume.getPublishedAt()
        );
    }

    private VisibilityStatus resolvePublicationVisibility(PublicationRequest request) {
        if (request.visibility() == VisibilityStatus.PUBLIC || request.visibility() == VisibilityStatus.PRIVATE) {
            return request.visibility();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publication visibility must be PUBLIC or PRIVATE");
    }
}
