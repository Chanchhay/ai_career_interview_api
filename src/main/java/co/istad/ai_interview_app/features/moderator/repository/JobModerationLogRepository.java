package co.istad.ai_interview_app.features.moderator.repository;

import co.istad.ai_interview_app.features.moderator.entity.JobModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobModerationLogRepository extends JpaRepository<JobModerationLog, UUID> {
}
