package co.istad.ai_interview_app.features.admin.audit.service;

import co.istad.ai_interview_app.features.admin.audit.repository.AuditLogRepository;
import co.istad.ai_interview_app.features.admin.entity.AuditLog;
import co.istad.ai_interview_app.shared.enums.admin.AuditActionType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Writes {@link AuditLog} rows for staff actions that change someone else's
 * account.
 *
 * <p>Each record is written in its own transaction ({@code REQUIRES_NEW}) so a
 * later failure in the calling service cannot roll the trail back: an attempt
 * that was made is worth recording even when it did not finish. For the same
 * reason a failure to write the record never propagates — losing an audit row
 * is bad, but failing the suspension it describes is worse.
 *
 * <p>Who performed the action is not stored here. {@code BaseEntity} already
 * fills {@code created_by} from the Spring Security auditor, and duplicating it
 * would give two fields that can disagree.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogRecorder {

    private static final int MAX_USER_AGENT_LENGTH = 255;

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            AuditActionType actionType,
            String entityName,
            String entityId,
            String description,
            Map<String, Object> oldValue,
            Map<String, Object> newValue
    ) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setActionType(actionType);
            auditLog.setEntityName(entityName);
            auditLog.setEntityId(entityId);
            auditLog.setDescription(description);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);

            HttpServletRequest request = currentRequest();
            if (request != null) {
                auditLog.setIpAddress(resolveClientIp(request));
                auditLog.setUserAgent(truncate(request.getHeader("User-Agent")));
            }

            auditLogRepository.save(auditLog);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to write audit log for {} on {} {}",
                    actionType,
                    entityName,
                    entityId,
                    exception
            );
        }
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }

    /**
     * The gateway sits in front of this service, so the socket address is the
     * gateway's. {@code X-Forwarded-For} holds the original client first.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }

        return forwardedFor.split(",")[0].trim();
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_USER_AGENT_LENGTH
                ? value
                : value.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
