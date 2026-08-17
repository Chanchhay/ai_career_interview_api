package co.istad.ai_interview_app.features.moderator.mapper;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.interview.human.entity.HumanInterview;
import co.istad.ai_interview_app.features.moderator.dto.ApplicationSummaryResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateApplicationReviewResponse;
import co.istad.ai_interview_app.features.moderator.dto.CandidateProfileResponse;
import co.istad.ai_interview_app.features.moderator.dto.HumanInterviewResponse;
import co.istad.ai_interview_app.features.moderator.dto.ProjectAssignmentSummaryResponse;
import co.istad.ai_interview_app.features.moderator.dto.SubmittedResumeResponse;
import co.istad.ai_interview_app.features.moderator.entity.CandidateApplicationReview;
import co.istad.ai_interview_app.features.project.entity.ProjectAssignment;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.features.seeker.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class CandidateApplicationMapper {

    public ApplicationSummaryResponse toApplicationSummary(JobApplication application) {
        return new ApplicationSummaryResponse(
                application.getId(),
                application.getJobPost().getId(),
                application.getJobPost().getTitle(),
                application.getCoverLetter(),
                application.getStatus(),
                application.getAppliedAt()
        );
    }

    public CandidateProfileResponse toCandidateProfile(JobSeekerProfile profile) {
        return new CandidateProfileResponse(
                profile.getId(),
                profile.getHeadline(),
                profile.getCurrentPosition(),
                profile.getPreferredLocation(),
                profile.getAvailabilityStatus()
        );
    }

    public SubmittedResumeResponse toSubmittedResume(Resume resume) {
        if (resume == null) {
            return null;
        }

        return new SubmittedResumeResponse(
                resume.getId(),
                resume.getTitle(),
                resume.getResumeFileUrl(),
                resume.getVisibility()
        );
    }

    public CandidateApplicationReviewResponse toReviewResponse(CandidateApplicationReview review) {
        return new CandidateApplicationReviewResponse(
                review.getId(),
                review.getReviewStatus(),
                review.getDecisionNote(),
                review.getReviewedAt(),
                review.getApprovedAt(),
                review.getForwardedAt()
        );
    }

    public HumanInterviewResponse toHumanInterviewResponse(HumanInterview interview) {
        return new HumanInterviewResponse(
                interview.getId(),
                interview.getApplication().getId(),
                interview.getScheduledAt(),
                interview.getMeetingUrl(),
                interview.getStatus(),
                interview.getResult(),
                interview.getNote(),
                interview.getCompletedAt(),
                interview.getCancelledAt()
        );
    }

    public ProjectAssignmentSummaryResponse toProjectAssignmentResponse(ProjectAssignment assignment) {
        return new ProjectAssignmentSummaryResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDeadlineAt(),
                assignment.getStatus()
        );
    }
}
