package co.istad.ai_interview_app.features.admin.audit.repository;

import co.istad.ai_interview_app.features.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByEntityNameAndEntityId(String entityName, String entityId, Pageable pageable);
}
