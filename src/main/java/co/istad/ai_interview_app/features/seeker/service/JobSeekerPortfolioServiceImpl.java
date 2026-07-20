package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.PortfolioCreateRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioProjectUpdateRequest;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioResponse;
import co.istad.ai_interview_app.features.seeker.dto.PortfolioUpdateRequest;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Portfolio;
import co.istad.ai_interview_app.features.seeker.entity.PortfolioProject;
import co.istad.ai_interview_app.features.seeker.mapper.PortfolioMapper;
import co.istad.ai_interview_app.features.seeker.repository.PortfolioProjectRepository;
import co.istad.ai_interview_app.features.seeker.repository.PortfolioRepository;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class JobSeekerPortfolioServiceImpl implements JobSeekerPortfolioService {

    private final AuthenticatedJobSeekerProfileResolver profileResolver;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioProjectRepository projectRepository;
    private final PortfolioMapper portfolioMapper;

    @Override
    @Transactional
    public PortfolioResponse createPortfolio(PortfolioCreateRequest request) {
        JobSeekerProfile profile = profileResolver.resolve();

        Portfolio portfolio = new Portfolio();
        portfolio.setJobSeekerProfile(profile);
        portfolio.setTitle(normalizeRequiredText(request.title(), "Title is required"));
        portfolio.setSummary(normalizeBlankToNull(request.summary()));
        portfolio.setPublicUrl(normalizeBlankToNull(request.publicUrl()));
        portfolio.setVisibility(VisibilityStatus.PRIVATE);
        portfolio.setStatus(ProfileStatus.ACTIVE);

        return toResponse(portfolioRepository.save(portfolio));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getMyPortfolios() {
        JobSeekerProfile profile = profileResolver.resolve();

        return portfolioRepository.findAllByJobSeekerProfile_IdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioResponse getMyPortfolio(Long portfolioId) {
        JobSeekerProfile profile = profileResolver.resolve();
        return toResponse(resolveOwnedPortfolio(portfolioId, profile.getId()));
    }

    @Override
    @Transactional
    public PortfolioResponse updatePortfolio(Long portfolioId, PortfolioUpdateRequest request) {
        JobSeekerProfile profile = profileResolver.resolve();
        Portfolio portfolio = resolveOwnedPortfolio(portfolioId, profile.getId());

        if (request.title() != null) {
            portfolio.setTitle(normalizeRequiredText(request.title(), "Title is required"));
        }
        if (request.summary() != null) {
            portfolio.setSummary(normalizeBlankToNull(request.summary()));
        }
        if (request.publicUrl() != null) {
            portfolio.setPublicUrl(normalizeBlankToNull(request.publicUrl()));
        }

        return toResponse(portfolio);
    }

    @Override
    @Transactional
    public void deletePortfolio(Long portfolioId) {
        JobSeekerProfile profile = profileResolver.resolve();
        Portfolio portfolio = resolveOwnedPortfolio(portfolioId, profile.getId());

        projectRepository.deleteAllByPortfolio_Id(portfolio.getId());
        portfolioRepository.delete(portfolio);
    }

    @Override
    @Transactional
    public PortfolioProjectResponse createProject(Long portfolioId, PortfolioProjectRequest request) {
        JobSeekerProfile profile = profileResolver.resolve();
        Portfolio portfolio = resolveOwnedPortfolio(portfolioId, profile.getId());

        PortfolioProject project = new PortfolioProject();
        project.setPortfolio(portfolio);
        project.setTitle(normalizeRequiredText(request.title(), "Title is required"));
        project.setDescription(normalizeBlankToNull(request.description()));
        project.setProjectUrl(normalizeBlankToNull(request.projectUrl()));
        project.setGithubUrl(normalizeBlankToNull(request.githubUrl()));
        project.setImageUrl(normalizeBlankToNull(request.imageUrl()));
        project.setTechStack(normalizeBlankToNull(request.techStack()));
        project.setDisplayOrder(request.displayOrder());

        return portfolioMapper.toProjectResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public PortfolioProjectResponse updateProject(Long portfolioId, Long projectId, PortfolioProjectUpdateRequest request) {
        JobSeekerProfile profile = profileResolver.resolve();
        Portfolio portfolio = resolveOwnedPortfolio(portfolioId, profile.getId());
        PortfolioProject project = resolveProject(projectId, portfolio.getId());

        if (request.title() != null) {
            project.setTitle(normalizeRequiredText(request.title(), "Title is required"));
        }
        if (request.description() != null) {
            project.setDescription(normalizeBlankToNull(request.description()));
        }
        if (request.projectUrl() != null) {
            project.setProjectUrl(normalizeBlankToNull(request.projectUrl()));
        }
        if (request.githubUrl() != null) {
            project.setGithubUrl(normalizeBlankToNull(request.githubUrl()));
        }
        if (request.imageUrl() != null) {
            project.setImageUrl(normalizeBlankToNull(request.imageUrl()));
        }
        if (request.techStack() != null) {
            project.setTechStack(normalizeBlankToNull(request.techStack()));
        }
        if (request.displayOrder() != null) {
            project.setDisplayOrder(request.displayOrder());
        }

        return portfolioMapper.toProjectResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long portfolioId, Long projectId) {
        JobSeekerProfile profile = profileResolver.resolve();
        Portfolio portfolio = resolveOwnedPortfolio(portfolioId, profile.getId());
        PortfolioProject project = resolveProject(projectId, portfolio.getId());

        projectRepository.delete(project);
    }

    private PortfolioResponse toResponse(Portfolio portfolio) {
        List<PortfolioProject> projects = projectRepository.findAllByPortfolio_IdOrderByDisplayOrderAscCreatedAtDesc(portfolio.getId());
        return portfolioMapper.toResponse(portfolio, projects);
    }

    private Portfolio resolveOwnedPortfolio(Long portfolioId, Long profileId) {
        return portfolioRepository.findByIdAndJobSeekerProfile_Id(portfolioId, profileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portfolio was not found for authenticated job seeker"
                ));
    }

    private PortfolioProject resolveProject(Long projectId, Long portfolioId) {
        return projectRepository.findByIdAndPortfolio_Id(projectId, portfolioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portfolio project was not found for authenticated job seeker"
                ));
    }

    private String normalizeRequiredText(String value, String message) {
        if (!hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
