package co.istad.ai_interview_app.features.moderator.repository;

import co.istad.ai_interview_app.features.moderator.entity.CandidateApplicationReview;
import co.istad.ai_interview_app.shared.enums.review.CandidateApplicationReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CandidateApplicationReviewRepository extends JpaRepository<CandidateApplicationReview, Long> {

    Optional<CandidateApplicationReview> findByApplication_Id(Long applicationId);

    @EntityGraph(attributePaths = {
            "application",
            "application.jobPost",
            "application.resume",
            "application.jobSeekerProfile",
            "application.jobSeekerProfile.userAccount",
            "moderator"
    })
    Optional<CandidateApplicationReview> findWithApplicationByApplication_Id(Long applicationId);

    @EntityGraph(attributePaths = {
            "application",
            "application.jobPost",
            "application.resume",
            "application.jobSeekerProfile",
            "application.jobSeekerProfile.userAccount",
            "moderator"
    })
    Page<CandidateApplicationReview> findAllByReviewStatus(CandidateApplicationReviewStatus reviewStatus, Pageable pageable);

    @EntityGraph(attributePaths = {
            "application",
            "application.jobPost",
            "application.resume",
            "application.jobSeekerProfile",
            "application.jobSeekerProfile.userAccount",
            "moderator"
    })
    Page<CandidateApplicationReview> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "application",
            "application.jobPost",
            "application.resume",
            "application.jobSeekerProfile",
            "application.jobSeekerProfile.userAccount"
    })
    List<CandidateApplicationReview> findAllByReviewStatusAndApplication_JobPost_RecruiterProfile_UserAccount_KeycloakUserIdOrderByForwardedAtDesc(
            CandidateApplicationReviewStatus reviewStatus,
            String keycloakUserId
    );

    @EntityGraph(attributePaths = {
            "application",
            "application.jobPost",
            "application.resume",
            "application.jobSeekerProfile",
            "application.jobSeekerProfile.userAccount"
    })
    Optional<CandidateApplicationReview> findByApplication_IdAndReviewStatusAndApplication_JobPost_RecruiterProfile_UserAccount_KeycloakUserId(
            Long applicationId,
            CandidateApplicationReviewStatus reviewStatus,
            String keycloakUserId
    );
}
