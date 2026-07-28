package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.entity.AccessAuditEvent;
import br.com.systemcommerce.access.repository.AccessAuditEventRepository;
import br.com.systemcommerce.shared.audit.AuditRequestContext;
import br.com.systemcommerce.shared.web.CorrelationIdContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessAuditEventService {

    private final AccessAuditEventRepository repository;

    @Transactional
    public void record(
            String eventType,
            AccessAuditEvent.Result result,
            UUID actorUserId,
            UUID targetUserId,
            UUID groupId,
            UUID permissionId,
            String permissionCode,
            String scope,
            String reason,
            String beforeData,
            String afterData,
            UUID organizationId,
            UUID storeId) {
        AccessAuditEvent e = new AccessAuditEvent();
        e.setEventType(eventType);
        e.setResult(result != null ? result.name() : AccessAuditEvent.Result.SUCCESS.name());
        e.setActorUserId(actorUserId);
        e.setTargetUserId(targetUserId);
        e.setGroupId(groupId);
        e.setPermissionId(permissionId);
        e.setPermissionCode(sanitize(permissionCode));
        e.setScope(scope);
        e.setReason(reason);
        e.setBeforeData(sanitizePayload(beforeData));
        e.setAfterData(sanitizePayload(afterData));
        e.setOrganizationId(organizationId);
        e.setStoreId(storeId);
        e.setIpAddress(AuditRequestContext.ipAddress());
        e.setCorrelationId(CorrelationIdContext.current());
        repository.save(e);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String lower = value.toLowerCase();
        if (lower.contains("password") || lower.contains("token") || lower.contains("secret")) {
            return "[REDACTED]";
        }
        return value;
    }

    private static String sanitizePayload(String json) {
        if (json == null) {
            return null;
        }
        return json.replaceAll("(?i)\"(password|passwordHash|token|secret|refreshToken)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"[REDACTED]\"");
    }
}
