package br.com.systemcommerce.shared.audit;

import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            UUID userId,
            String module,
            AuditLog.AuditAction action,
            String entityName,
            UUID storeId,
            UUID organizationId,
            java.time.Instant from,
            java.time.Instant to,
            Pageable pageable) {
        UUID currentUserId = CurrentUser.requireId();
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(currentUserId, storeId);
        }
        Collection<UUID> allowedStoreIds = resolveAccessibleStoreFilter(storeId, organizationId);
        return auditLogRepository
                .findAll(
                        AuditLogSpecifications.withFilters(
                                userId,
                                module,
                                action,
                                entityName,
                                storeId,
                                organizationId,
                                allowedStoreIds,
                                from,
                                to),
                        pageable)
                .map(this::toResponse);
    }

    private Collection<UUID> resolveAccessibleStoreFilter(UUID storeId, UUID organizationId) {
        if (storeId != null || organizationId != null) {
            return null;
        }
        if (storeAuthorizationEvaluator.hasGlobalAccess()) {
            return null;
        }
        List<UUID> ids = storeAuthorizationEvaluator.listEffectiveAccess(CurrentUser.requireId()).stream()
                .map(UserStoreAccess::getStore)
                .map(s -> s.getId())
                .toList();
        return ids;
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getModule(),
                log.getEntityName(),
                log.getEntityId(),
                log.getAction().name(),
                parseJson(log.getOldValues()),
                parseJson(log.getNewValues()),
                log.getDetails(),
                log.getIpAddress(),
                log.getCorrelationId(),
                log.getStoreId(),
                log.getOrganizationId(),
                log.getPerformedBy() != null ? log.getPerformedBy().getId() : null,
                log.getPerformedBy() != null ? log.getPerformedBy().getName() : null,
                log.getPerformedBy() != null ? log.getPerformedBy().getLogin() : null,
                log.getPerformedAt());
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            return json;
        }
    }
}
