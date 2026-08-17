package co.istad.ai_interview_app.features.application.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.application.dto.JobApplicationCreateRequest;
import co.istad.ai_interview_app.features.application.dto.JobApplicationResponse;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.application.mapper.JobApplicationMapper;
import co.istad.ai_interview_app.features.application.repository.JobApplicationRepository;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.repository.JobPostRepository;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.features.seeker.repository.ResumeRepository;
import co.istad.ai_interview_app.features.seeker.service.AuthenticatedJobSeekerProfileResolver;
import co.istad.ai_interview_app.shared.enums.application.ApplicationStatus;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class JobSeekerApplicationServiceImpl implements JobSeekerApplicationService {

    private final AuthenticatedJobSeekerProfileResolver seekerProfileResolver;
    private final JobPostRepository jobPostRepository;
    private final ResumeRepository resumeRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobApplicationMapper applicationMapper;

    @Override
    @Transactional
    public JobApplicationResponse apply(Long jobId, JobApplicationCreateRequest request) {
        JobSeekerProfile seekerProfile = seekerProfileResolver.resolve();
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job post was not found"));
        validateJobAcceptsApplications(jobPost);

        if (applicationRepository.existsByJobPost_IdAndJobSeekerProfile_Id(jobPost.getId(), seekerProfile.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied to this job");
        }

        JobApplication application = new JobApplication();
        application.setJobPost(jobPost);
        application.setJobSeekerProfile(seekerProfile);
        application.setResume(resolveOwnedResume(request.resumeId(), seekerProfile.getId()));
        application.setCoverLetter(normalizeBlankToNull(request.coverLetter()));
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setAppliedAt(Instant.now());

        try {
            return applicationMapper.toResponse(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied to this job");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getMyApplications() {
        return applicationRepository.findAllByJobSeekerProfile_UserAccount_KeycloakUserIdOrderByAppliedAtDesc(AuthUtils.extractUserId())
                .stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationResponse getMyApplication(Long applicationId) {
        return applicationMapper.toResponse(resolveMyApplication(applicationId));
    }

    @Override
    @Transactional
    public JobApplicationResponse withdraw(Long applicationId) {
        JobApplication application = resolveMyApplication(applicationId);
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            return applicationMapper.toResponse(application);
        }
        if (application.getStatus() == ApplicationStatus.HIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hired applications cannot be withdrawn");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        return applicationMapper.toResponse(application);
    }

    private JobApplication resolveMyApplication(Long applicationId) {
        return applicationRepository.findByIdAndJobSeekerProfile_UserAccount_KeycloakUserId(applicationId, AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application was not found for authenticated job seeker"
                ));
    }

    private Resume resolveOwnedResume(Long resumeId, Long seekerProfileId) {
        if (resumeId == null) {
            return null;
        }

        return resumeRepository.findByIdAndJobSeekerProfile_Id(resumeId, seekerProfileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume was not found for authenticated job seeker"
                ));
    }

    private void validateJobAcceptsApplications(JobPost jobPost) {
        if (jobPost.getStatus() != JobStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only published jobs accept applications");
        }
        if (jobPost.getExpiredAt() != null && !jobPost.getExpiredAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expired jobs do not accept applications");
        }
    }
}
