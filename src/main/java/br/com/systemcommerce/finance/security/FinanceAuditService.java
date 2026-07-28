package br.com.systemcommerce.finance.security;

import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FinanceAuditService {

    private final DomainAuditService domainAuditService;

    public void success(String eventCode, String entityName, UUID entityId, AuditLog.AuditAction action, String details) {
        domainAuditService.recordEvent(
                "FINANCE", entityName, entityId, action, eventCode, "SUCCESS", null, null, details);
    }

    public void success(
            String eventCode,
            String entityName,
            UUID entityId,
            AuditLog.AuditAction action,
            Map<String, ?> after,
            String details) {
        domainAuditService.recordEvent(
                "FINANCE", entityName, entityId, action, eventCode, "SUCCESS", null, after, details);
    }

    public void denied(String entityName, UUID entityId, String details) {
        domainAuditService.recordEvent(
                "FINANCE",
                entityName,
                entityId,
                AuditLog.AuditAction.OTHER,
                FinanceAuditEvents.DENIED_ACCESS,
                "DENIED",
                null,
                null,
                details);
    }
}
