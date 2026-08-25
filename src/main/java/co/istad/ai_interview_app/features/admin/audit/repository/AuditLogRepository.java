package co.istad.ai_interview_app.features.admin.audit.repository;

import co.istad.ai_interview_app.features.admin.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Write-only for now: {@code AuditLogRecorder} appends, and nothing reads.
 *
 * <p>Deliberately bare. Query methods for a read API that does not exist yet
 * would be untested code shaped by a guess about how the trail will be browsed.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
