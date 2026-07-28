package br.com.systemcommerce.shared.audit;

import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.shared.web.CorrelationIdContext;
import br.com.systemcommerce.storecontext.CurrentStoreContext;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainAuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(
            String entityName,
            UUID entityId,
            AuditLog.AuditAction action,
            Object oldValues,
            Object newValues,
            String details) {
        record(deriveModule(entityName), entityName, entityId, action, oldValues, newValues, details, null);
    }

    @Transactional
    public void record(
            String module,
            String entityName,
            UUID entityId,
            AuditLog.AuditAction action,
            Object oldValues,
            Object newValues,
            String details) {
        record(module, entityName, entityId, action, oldValues, newValues, details, null);
    }

    @Transactional
    public void record(
            String module,
            String entityName,
            UUID entityId,
            AuditLog.AuditAction action,
            Object oldValues,
            Object newValues,
            String details,
            User explicitActor) {
        persist(module, entityName, entityId, action, oldValues, newValues, details, explicitActor, null, null);
    }

    /** Auditoria financeira com eventCode/outcome (Prompt 119). */
    @Transactional
    public void recordEvent(
            String module,
            String entityName,
            UUID entityId,
            AuditLog.AuditAction action,
            String eventCode,
            String outcome,
            Object oldValues,
            Object newValues,
            String details) {
        persist(module, entityName, entityId, action, oldValues, newValues, details, null, eventCode, outcome);
    }

    /**
     * Grava em transação independente — necessário para LOGIN_FAILURE (e demais eventos de auth)
     * quando a transação externa faz rollback por UnauthorizedException.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuth(
            String entityName,
            UUID entityId,
            AuditLog.AuditAction action,
            Object oldValues,
            Object newValues,
            String details,
            User explicitActor) {
        persist("AUTH", entityName, entityId, action, oldValues, newValues, details, explicitActor, null, null);
    }

    public void recordCustomer(
            UUID id, AuditLog.AuditAction action, Map<String, ?> oldValues, Map<String, ?> newValues, String details) {
        record("CUSTOMER", "Customer", id, action, oldValues, newValues, details);
    }

    private void persist(
            String module,
            String entityName,
            UUID entityId,
            AuditLog.AuditAction action,
            Object oldValues,
            Object newValues,
            String details,
            User explicitActor,
            String eventCode,
            String outcome) {
        AuditLog entry = new AuditLog();
        entry.setModule(normalizeModule(module, entityName));
        entry.setEntityName(entityName);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setOldValues(toJson(AuditSanitizer.sanitize(oldValues)));
        entry.setNewValues(toJson(AuditSanitizer.sanitize(newValues)));
        entry.setDetails(truncate(details, 1000));
        entry.setIpAddress(truncate(AuditRequestContext.ipAddress(), 45));
        entry.setCorrelationId(truncate(CorrelationIdContext.current(), 100));
        entry.setEventCode(truncate(eventCode, 80));
        entry.setOutcome(truncate(outcome, 20));
        entry.setPerformedBy(resolveActor(explicitActor));
        applyStoreContext(entry);
        auditLogRepository.save(entry);
        log.info(
                "Audit module={} entity={} id={} action={} event={} outcome={} user={} correlationId={}",
                entry.getModule(),
                entityName,
                entityId,
                action,
                eventCode,
                outcome,
                entry.getPerformedBy() != null ? entry.getPerformedBy().getId() : null,
                entry.getCorrelationId());
    }

    private User resolveActor(User explicitActor) {
        if (explicitActor != null) {
            return explicitActor;
        }
        return CurrentUser.id().flatMap(userRepository::findById).orElse(null);
    }

    private static void applyStoreContext(AuditLog entry) {
        CurrentStoreContext ctx = CurrentStoreContext.get();
        if (!ctx.hasStore()) {
            return;
        }
        if (entry.getStoreId() == null) {
            entry.setStoreId(ctx.storeId());
        }
        if (entry.getOrganizationId() == null) {
            entry.setOrganizationId(ctx.organizationId());
        }
    }

    static String deriveModule(String entityName) {
        if (!StringUtils.hasText(entityName)) {
            return "OTHER";
        }
        return switch (entityName.trim().toLowerCase(Locale.ROOT)) {
            case "customer" -> "CUSTOMER";
            case "product", "category" -> "PRODUCT";
            case "sale" -> "SALE";
            case "payment" -> "PAYMENT";
            case "user" -> "USER";
            case "role", "permission" -> "USER";
            case "inventory", "inventorymovement", "stockmovement" -> "INVENTORY";
            case "auth", "session" -> "AUTH";
            default -> entityName.trim().toUpperCase(Locale.ROOT);
        };
    }

    private static String normalizeModule(String module, String entityName) {
        if (StringUtils.hasText(module)) {
            return module.trim().toUpperCase(Locale.ROOT);
        }
        return deriveModule(entityName);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
