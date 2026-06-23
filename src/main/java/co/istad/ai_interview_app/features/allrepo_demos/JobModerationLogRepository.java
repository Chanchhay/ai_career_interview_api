package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.moderator.entity.JobModerationLog;
import co.istad.ai_interview_app.shared.enums.ModerationDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface JobModerationLogRepository extends JpaRepository<JobModerationLog, Long> {

    List<JobModerationLog> findByJobPost_Id(Long jobPostId);

    List<JobModerationLog> findByModeratorProfile_Id(Long moderatorProfileId);

    List<JobModerationLog> findByAction(ModerationDecision action);
}
